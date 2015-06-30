package com.example.hsxu.typeracing;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.IdRes;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.app.Activity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;
import com.google.example.games.basegameutils.BaseGameActivity;
import com.google.android.gms.games.multiplayer.Multiplayer;

import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LoginActivity extends BaseGameActivity implements
        View.OnClickListener, RoomUpdateListener, RealTimeMessageReceivedListener,
        RoomStatusUpdateListener {
    private static final String TAG = "MultiplayerActivity";

    /* For the game */
    private TextView mStatus;

    private EditText mTypingBox;
    private TextView mMessageBox;
    private Button mRestartButton;
    private Button mSendToLeaderboardsButton;

    private String[] mMessageToType;
    private int mWordIndex = 0;
    private boolean mIsGameFinished = false;

    private GameEngine mGameEngine;

    private int mProgressStatusNum = 0;
    private Handler mHandler = new Handler();

    private com.google.android.gms.common.SignInButton mSignInButton;
    private Button mSignOutButton;

    final static int[] requiresClickListeners = {
        R.id.sign_in_button,
        R.id.sign_out_button,
        R.id.quick_match_button,
        R.id.invite_players_button,
        R.id.view_invitations_button,
        R.id.restartButton,
        R.id.sendToLeaderboardsButton
    };

    final static int[] requiresTouchListeners = {
        R.id.sign_out_button,
        R.id.quick_match_button,
        R.id.invite_players_button,
        R.id.view_invitations_button
    };

    final static int[] screens = {
            R.id.menu_screen,
            R.id.multiplayer_game_screen,
            R.id.waiting_screen
    };

    // request code for the "select players" UI
    // can be any number as long as it's unique
    final int RC_SELECT_PLAYERS = 10000;
    final int RC_INVITATION_INBOX = 10001;
    final int RC_WAITING_ROOM = 10002;

    final static int MIN_PLAYERS = 2;

    private String mRoomId = null;
    //Participants in the currently active game
    private ArrayList<Participant> mParticipants = new ArrayList<Participant>();
    //Progress of others
    private Map<String, Integer> mParticipantProgress = new HashMap<String, Integer>();
    //WPM of others
    private Map<String, Integer> mParticipantWPM = new HashMap<String, Integer>();
    // Participants who sent us their final score.
    private Set<String> mFinishedParticipants = new HashSet<String>();
    //A map of participants to their index
    private HashMap<String, Integer> mParticipantIndexes;
    //My participant id in the current active game
    private String mMyId = null;
     //Id of the invitation we received via invitation listener
    private String mIncomingInvitationId = null;

    private TextView mSignInStatus;

    @IdRes private final int ONE = 1;
    @IdRes private final int TWO = 2;
    @IdRes private int mOpponentIndex = 2;

    TextView mUserWPM;
    ProgressBar mUserProgress;

    private boolean mIsUserFinishedGame = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().hide();
        setContentView(R.layout.activity_login);
        mSignInStatus = (TextView)findViewById(R.id.signed_in_status);
        for (int id : requiresClickListeners) {
            findViewById(id).setOnClickListener(this);
        }
        for (int id : requiresTouchListeners) {
            findViewById(id).setOnTouchListener(new GoldBorderOnTouchListener((Button) findViewById(id)));
        }

        setCurrentScreen(R.id.menu_screen);

        mStatus = (TextView)findViewById(R.id.status);
        mTypingBox = (EditText)findViewById(R.id.typingBox);
        mMessageBox = (TextView)findViewById(R.id.messageBox);

        mRestartButton = (Button)findViewById(R.id.restartButton);
        mRestartButton.setVisibility(View.GONE);

        mSendToLeaderboardsButton = (Button)findViewById(R.id.sendToLeaderboardsButton);

        mSignInButton = (com.google.android.gms.common.SignInButton)findViewById(R.id.sign_in_button);
        mSignOutButton = (Button)findViewById(R.id.sign_out_button);

        mMessageToType = mMessageBox.getText().toString().split(" ");

        mTypingBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                mGameEngine.runMainGameEngine(charSequence);
            }
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {}
            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.sign_in_button: {
                beginUserInitiatedSignIn();
                updateSignInStatus("Signing in...", Color.parseColor("#FFBB33"));
                break;
            }
            case R.id.sign_out_button: {
                signOut();
                //show sign-in, hide sign-out.
                mSignInButton.setVisibility(View.VISIBLE);
                mSignOutButton.setVisibility(View.GONE);
                updateSignInStatus("Signed out", Color.parseColor("#FF4444"));
                break;
            }
            case R.id.quick_match_button: {
                if (getApiClient().isConnected()) {
                    setCurrentScreen(R.id.waiting_screen);
                    startQuickGame();
                } else {
                    createToast("Please sign in to access quick matches.");
                }
                break;
            }
            case R.id.invite_players_button: {
                if (getApiClient().isConnected()) {
                    invitePlayers();
                } else {
                    createToast("Please sign in to invite players.");
                }
                break;
            }
            case R.id.view_invitations_button: {
                if (getApiClient().isConnected()) {
                    setCurrentScreen(R.id.multiplayer_game_screen);
                } else {
                    createToast("Please sign in to view invitations.");
                }
                break;
            }
            case R.id.restartButton: {
                mGameEngine.restartGame();
                startMultiplayerGameCycle();
                break;
            }
        }
    }

    private void updateSignInStatus(String status, int color) {
        mSignInStatus.setText(status);
        mSignInStatus.setTextColor(color);
    }

    private void createToast(String text) {
        Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    private void acceptInviteToRoom(String invitationId) {
        RoomConfig.Builder roomConfigBuilder = makeBasicRoomConfigBuilder();
        roomConfigBuilder.setInvitationIdToAccept(invitationId);
        Games.RealTimeMultiplayer.join(getApiClient(), roomConfigBuilder.build());
        // prevent screen from sleeping during handshake
        keepScreenOn();
    }


    public void startQuickGame() {
        Log.d(TAG, "startQuickGame() - Started quick game.");
        // You can also specify more opponents (up to 3).
        Bundle am = RoomConfig.createAutoMatchCriteria(1, 1, 0);

        // build the room config:
        RoomConfig.Builder roomConfigBuilder = makeBasicRoomConfigBuilder();
        roomConfigBuilder.setAutoMatchCriteria(am);
        RoomConfig roomConfig = roomConfigBuilder.build();

        // create room:
        Games.RealTimeMultiplayer.create(getApiClient(), roomConfig);

        // prevent screen from sleeping during handshake
        keepScreenOn();
    }

    public void invitePlayers() {
        // minimum: 1 other player; maximum: 3 other players
        final int MIN_PLAYERS = 1;
        final int MAX_PLAYERS = 3;
        Intent intent = Games.RealTimeMultiplayer.getSelectOpponentsIntent(getApiClient(),
                MIN_PLAYERS, MAX_PLAYERS);
        startActivityForResult(intent, RC_SELECT_PLAYERS);
    }

    @Override
    protected void onActivityResult(int request, int response, Intent data) {
        super.onActivityResult(request, response, data);
        if (request == RC_SELECT_PLAYERS) {
            if (response != Activity.RESULT_OK) {
                // user canceled
                return;
            }

            // get the invitee list
            Bundle extras = data.getExtras();
            final ArrayList<String> invitees =
                    data.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);

            // get auto-match criteria
            Bundle autoMatchCriteria = null;
            int minAutoMatchPlayers =
                    data.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
            int maxAutoMatchPlayers =
                    data.getIntExtra(Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);

            if (minAutoMatchPlayers > 0) {
                autoMatchCriteria = RoomConfig.createAutoMatchCriteria(
                        minAutoMatchPlayers, maxAutoMatchPlayers, 0);
            } else {
                autoMatchCriteria = null;
            }

            // create the room and specify a variant if appropriate
            RoomConfig.Builder roomConfigBuilder = makeBasicRoomConfigBuilder();
            roomConfigBuilder.addPlayersToInvite(invitees);
            if (autoMatchCriteria != null) {
                roomConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
            }
            RoomConfig roomConfig = roomConfigBuilder.build();
            Games.RealTimeMultiplayer.create(getApiClient(), roomConfig);

            // prevent screen from sleeping during handshake
            keepScreenOn();
        }

        else if (request == RC_WAITING_ROOM) {

            if (response == Activity.RESULT_OK) {
                mOpponentIndex = TWO;
                TextView participantName;
                mParticipantIndexes = new HashMap<String, Integer>();

                for (Participant participant : mParticipants) {
                    if (participant.getParticipantId().equals(mMyId)) {
                        continue;
                    }
                    addOpponentProgressBars(mOpponentIndex);
                    participantName = (TextView)findViewById(mOpponentIndex);
                    participantName.setText(participant.getDisplayName());
                    mParticipantIndexes.put(participant.getParticipantId(), mOpponentIndex);
                    mParticipantProgress.put(participant.getParticipantId(), 0);
                    mParticipantWPM.put(participant.getParticipantId(), 0);
                    mOpponentIndex++;
                }

                for (Participant participant : mParticipants) {
                    if (participant.getParticipantId().equals(mMyId)) {
                        addOpponentProgressBars(ONE);
                    }
                }
                //Should never fail because it's the original user's.
                TextView userFullName = (TextView)findViewById(ONE);
                userFullName.setText("You");
                mUserWPM = (TextView)findViewById(ONE * 10);
                mUserProgress = (ProgressBar)findViewById(ONE * 100);

                mGameEngine = new GameEngine(mTypingBox, mMessageBox, mUserWPM, mRestartButton,
                        mMessageToType, mWordIndex, mStatus,
                        mSendToLeaderboardsButton);

                setCurrentScreen(R.id.multiplayer_game_screen);
                startMultiplayerGameCycle();
            }

            else if (response == Activity.RESULT_CANCELED ||
                     response == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
                // Waiting room was dismissed with the back button || player wanted to leave the room.

                Games.RealTimeMultiplayer.leave(getApiClient(), this, mRoomId);
                mRoomId = null;
                keepScreenOn();
            }
        }
    }

    private void addOpponentProgressBars(int oneBasedIndex) {
        LinearLayout parentLayout = (LinearLayout)findViewById(R.id.game_container);

        LinearLayout newLayout = new LinearLayout(this);
        LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        //Set up name TextView
        TextView nameTv = new TextView(this);
        LinearLayout.LayoutParams nameTvParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                0.2f
        );

        nameTv.setText("You");
        nameTv.setGravity(Gravity.CENTER);
        nameTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        nameTv.setBackgroundColor(Color.parseColor("#AA2c3e50"));
        nameTv.setTextColor(Color.WHITE);
        nameTv.setId(oneBasedIndex);

        //Set up WPM TextView
        TextView wpmTv = new TextView(this);
        LinearLayout.LayoutParams wpmTvParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                0.2f
        );

        wpmTv.setText("WPM: 0");
        wpmTv.setGravity(Gravity.CENTER);
        wpmTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        wpmTv.setBackgroundColor(Color.parseColor("#AAFF4444"));
        wpmTv.setTextColor(Color.WHITE);
        wpmTv.setId(oneBasedIndex * 10);

        //Set up Progressbar
        ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        LinearLayout.LayoutParams progressBarParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                0.6f
        );

        progressBarParams.gravity = Gravity.CENTER_HORIZONTAL;
        progressBar.setBackgroundColor(Color.parseColor("#AAd8d8d8"));
        progressBar.setPadding(10,0,10,0);
        progressBar.getProgressDrawable().setColorFilter(Color.parseColor("#CC0000"), PorterDuff.Mode.SRC_IN);
        progressBar.setId(oneBasedIndex * 100);

        newLayout.addView(nameTv, nameTvParams);
        newLayout.addView(wpmTv, wpmTvParams);
        newLayout.addView(progressBar, progressBarParams);

        parentLayout.addView(newLayout, 0, linearLayoutParams);
    }

    private RoomConfig.Builder makeBasicRoomConfigBuilder() {
        return RoomConfig.builder(this).setMessageReceivedListener(this).setRoomStatusUpdateListener(this);
    }

    private boolean isMultiplayerGameFinished() {
        return mIsGameFinished && mFinishedParticipants.size() == mParticipants.size();
    }

    private void startMultiplayerGameCycle() {
        new Thread(new Runnable() {
            public void run() {
                mProgressStatusNum = 0;

                while (isMultiplayerGameFinished() == false) {
                    //We can't use local mWordIndex because of threading issues
                    double wordIndex = (double)mGameEngine.getWordIndex();
                    double msgLength = (double)mMessageToType.length;
                    double tempProgressConvert =  (wordIndex / msgLength) * 100;

                    mProgressStatusNum = (int)tempProgressConvert;
                    mHandler.post(new Runnable() {
                        public void run() {
                            if (mIsUserFinishedGame == false) {
                                mUserProgress.setProgress(mProgressStatusNum);
                                mUserWPM.setText("WPM: " + mGameEngine.calcRoundedWPM());

                                if (getApiClient().isConnected()) {
                                    if (mProgressStatusNum != 100) {
                                        broadcastScore(false, mProgressStatusNum, mGameEngine.calcRoundedWPM());
                                    } else {
                                        broadcastScore(true, mProgressStatusNum, mGameEngine.calcRoundedWPM());
                                        mIsUserFinishedGame = true;
                                    }
                                }
                            }
                            updateParticipantScores();
                        }
                    });

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void updateParticipantScores() {
        TextView participantWPM;
        ProgressBar participantProgress;

        for (Participant participant : mParticipants) {
            String participantId = participant.getParticipantId();
            if (participantId.equals(mMyId)) {
                continue;
            }

            int indexOfParticipant = mParticipantIndexes.get(participantId);

            participantWPM = (TextView)findViewById(indexOfParticipant * 10);
            participantWPM.setText("WPM: " + mParticipantWPM.get(participantId));

            participantProgress = (ProgressBar)findViewById(indexOfParticipant * 100);
            participantProgress.setProgress(mParticipantProgress.get(participantId));
        }
    }

    private void broadcastScore(boolean finalScore, int progressStatusNum, int wpm) {
        byte[] msgBuffer = new byte[3];
        //1st byte -> Indicates if it's final score or not
        msgBuffer[0] = (byte) (finalScore ? 'F' : 'U');
        //2nd byte -> Sends the progress/100.
        msgBuffer[1] = (byte) progressStatusNum;
        //3rd byte -> Sends wpm
        msgBuffer[2] = (byte) wpm;

        for (Participant p : mParticipants) {
            if (p.getParticipantId().equals(mMyId) || p.getStatus() != Participant.STATUS_JOINED) {
                continue;
            }
            if (finalScore) {
                Games.RealTimeMultiplayer.sendReliableMessage(getApiClient(), null, msgBuffer, mRoomId, p.getParticipantId());
            } else {
                Games.RealTimeMultiplayer.sendUnreliableMessage(getApiClient(), msgBuffer, mRoomId, p.getParticipantId());
            }
        }
    }

    @Override
    public void onRealTimeMessageReceived(RealTimeMessage realTimeMessage) {
        byte msgBuffer[] = realTimeMessage.getMessageData();
        String sender = realTimeMessage.getSenderParticipantId();

        if (msgBuffer[0] == 'F' || msgBuffer[0] == 'U') {
            int existingProgress = mParticipantProgress.containsKey(sender) ? mParticipantProgress.get(sender) : 0;
            int thisProgress = (int)msgBuffer[1];
            int wpm = (int)msgBuffer[2];

            mParticipantWPM.put(sender, wpm);
            //packets may arrive out of order so we check
            if (thisProgress > existingProgress) {
                mParticipantProgress.put(sender, thisProgress);
            }

            if ((char)msgBuffer[0] == 'F') {
                mFinishedParticipants.add(realTimeMessage.getSenderParticipantId());
            }
        }
    }

    @Override
    public void onSignInFailed() {
        mSignInButton.setVisibility(View.VISIBLE);
        mSignOutButton.setVisibility(View.GONE);
        updateSignInStatus("Signed out", Color.parseColor("#FF4444"));
        Log.d(TAG, "onSignInFailed() - Sign in failed");
    }

    @Override
    public void onSignInSucceeded() {
        mSignInButton.setVisibility(View.GONE);
        mSignOutButton.setVisibility(View.VISIBLE);
        updateSignInStatus("Signed in", Color.parseColor("#669900"));

        if (getInvitationId() != null) {
            acceptInviteToRoom(getInvitationId());
        }
        Log.d(TAG, "onSignInSucceeded() - Sign in succeeded");
    }

    @Override
    public void onRoomCreated(int statusCode, Room room) {
        Log.d(TAG, "onRoomCreated - room created.");
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            stopKeepingScreenOn();
            // TODO show error message, return to main screen.
        }

        if (mRoomId == null) {
            mRoomId = room.getRoomId();
        }

        final int MIN_PLAYERS_BEFORE_START = Integer.MAX_VALUE;
        Intent intent = Games.RealTimeMultiplayer.getWaitingRoomIntent(getApiClient(), room, MIN_PLAYERS_BEFORE_START);
        startActivityForResult(intent, RC_WAITING_ROOM);
    }

    @Override
    public void onJoinedRoom(int statusCode, Room room) {
        Log.d(TAG, "onJoinedRoom - joined room");
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            stopKeepingScreenOn();
            // TODO show error message, return to main screen.
        }

        if (mRoomId == null) {
            mRoomId = room.getRoomId();
        }
        /*The third parameter in getWaitingRoomIntent() indicates the number of players
        that must be connected in the room before the Start playing option is displayed.*/
        Intent intent = Games.RealTimeMultiplayer.getWaitingRoomIntent(getApiClient(), room, Integer.MAX_VALUE);
        startActivityForResult(intent, RC_WAITING_ROOM);
    }

    @Override
    public void onLeftRoom(int statusCode, String roomId) {
        Log.d(TAG, "onLeftRoom, code " + statusCode);
        setCurrentScreen(R.id.menu_screen);
    }

    @Override
    public void onRoomConnected(int statusCode, Room room) {
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            stopKeepingScreenOn();
            // TODO show error message, return to main screen.
        }
        updateRoom(room);
    }

    @Override
    public void onConnectedToRoom(Room room) {
        Log.d(TAG, "onConnectedToRoom.");

        // get room ID, participants and my ID:
        mRoomId = room.getRoomId();
        mParticipants = room.getParticipants();
        mMyId = room.getParticipantId(Games.Players.getCurrentPlayerId(getApiClient()));
    }

    @Override
    public void onDisconnectedFromRoom(Room room) {
        Log.d(TAG, "onDisconnectedFromRoom() - disconnected from room");
        // leave the room
        Games.RealTimeMultiplayer.leave(getApiClient(), this, mRoomId);
        mRoomId = null;
        // clear the flag that keeps the screen on
        stopKeepingScreenOn();

        // TODO show error message and return to main screen
    }

    @Override
    public void onPeerDeclined(Room room, List<String> peers) { updateRoom(room); }
    @Override
    public void onPeerInvitedToRoom(Room room, List<String> peers) { updateRoom(room); }
    @Override
    public void onPeerJoined(Room room, List<String> peers) { updateRoom(room); }
    @Override
    public void onPeerLeft(Room room, List<String> peers) { updateRoom(room); }
    @Override
    public void onRoomConnecting(Room room) { updateRoom(room); }
    @Override
    public void onRoomAutoMatching(Room room) { updateRoom(room); }
    @Override
    public void onPeersConnected(Room room, List<String> peers) { updateRoom(room); }
    @Override
    public void onPeersDisconnected(Room room, List<String> peers) { updateRoom(room); }
    @Override
    public void onP2PDisconnected(String participant) { }
    @Override
    public void onP2PConnected(String participant) { }

    private void updateRoom(Room room) {
        if (room != null) {
            mParticipants = room.getParticipants();
        }
    }

    private void keepScreenOn() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void stopKeepingScreenOn() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void setCurrentScreen(int screenId) {
        for (int id : screens) {
            findViewById(id).setVisibility(screenId == id ? View.VISIBLE : View.GONE);
        }
    }
}
