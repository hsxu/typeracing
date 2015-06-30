package com.example.hsxu.typeracing;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.games.Games;
import com.google.example.games.basegameutils.BaseGameActivity;

public class singleplayer_activity extends BaseGameActivity implements View.OnClickListener{
    private TextView mStatus;

    private EditText mTypingBox;
    private TextView mMessageBox;
    private TextView mWPM;
    private Button mRestartButton;
    private Button mSendToLeaderboardsButton;

    private String[] mMessageToType;
    private int mWordIndex = 0;

    private GameEngine mGameEngine;

    private ProgressBar mProgressBar;
    private int mProgressStatusNum = 0;
    private Handler mHandler = new Handler();

    private com.google.android.gms.common.SignInButton mSignInButton;
    private Button mSignOutButton;

    private static String LEADERBOARD_ID = "CgkIlKKJyP4WEAIQAg";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().hide();
        setContentView(R.layout.activity_singleplayer_activity);

        mStatus = (TextView)findViewById(R.id.status);
        mTypingBox = (EditText)findViewById(R.id.typingBox);
        mMessageBox = (TextView)findViewById(R.id.messageBox);
        mWPM = (TextView)findViewById(R.id.WPM);
        mProgressBar = (ProgressBar)findViewById(R.id.progressBar);

        mRestartButton = (Button)findViewById(R.id.restartButton);
        mRestartButton.setVisibility(View.GONE);
        mRestartButton.setOnClickListener(this);
        mRestartButton.setOnTouchListener(new DarkNavyOnTouchListener((Button) mRestartButton));

        mSendToLeaderboardsButton = (Button)findViewById(R.id.sendToLeaderboardsButton);
        mSendToLeaderboardsButton.setOnClickListener(this);
        mSendToLeaderboardsButton.setOnTouchListener(new DarkNavyOnTouchListener((Button) mSendToLeaderboardsButton));

        mSignInButton = (com.google.android.gms.common.SignInButton)findViewById(R.id.sign_in_button);
        mSignInButton.setOnClickListener(this);

        mSignOutButton = (Button)findViewById(R.id.sign_out_button);
        mSignOutButton.setOnClickListener(this);
        mSignOutButton.setOnTouchListener(new DarkNavyOnTouchListener((Button) mSignOutButton));

        getGameHelper().setMaxAutoSignInAttempts(0);

        mMessageToType = mMessageBox.getText().toString().split(" ");

        mGameEngine = new GameEngine(mTypingBox, mMessageBox, mWPM, mRestartButton,
                mMessageToType, mWordIndex, mStatus, mSendToLeaderboardsButton);

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

        mProgressBar.getProgressDrawable().setColorFilter(Color.parseColor("#CC0000"), PorterDuff.Mode.SRC_IN);
        startSingleplayerGameCycle();
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.sign_in_button: {
                beginUserInitiatedSignIn();
                break;
            }
            case R.id.sign_out_button: {
                signOut();
                mSignInButton.setVisibility(View.VISIBLE);
                mSignOutButton.setVisibility(View.GONE);
                break;
            }
            case R.id.restartButton: {
                mGameEngine.restartGame();
                startSingleplayerGameCycle();
                break;
            }
            case R.id.sendToLeaderboardsButton: {
                if (getApiClient().isConnected()) {
                    Games.Leaderboards.submitScore(getApiClient(), LEADERBOARD_ID, mGameEngine.getFinalWPM());
                } else {
                    Toast toast = Toast.makeText(this, "Please sign in to send to leaderboards.", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
            }
        }
    }

    @Override
    public void onSignInFailed() {
        mSignInButton.setVisibility(View.VISIBLE);
        mSignOutButton.setVisibility(View.GONE);
    }

    @Override
    public void onSignInSucceeded() {
        mSignInButton.setVisibility(View.GONE);
        mSignOutButton.setVisibility(View.VISIBLE);
    }

    private void startSingleplayerGameCycle() {
        new Thread(new Runnable() {
            public void run() {
                mProgressStatusNum = 0;

                while (mProgressStatusNum < 100){
                    //We can't use local mWordIndex because of threading issues
                    double wordIndex = (double)mGameEngine.getWordIndex();
                    double msgLength = (double)mMessageToType.length;
                    double tempProgressConvert =  (wordIndex / msgLength) * 100;
                    mProgressStatusNum = (int)tempProgressConvert;

                    mHandler.post(new Runnable() {
                        public void run() {
                            mProgressBar.setProgress(mProgressStatusNum);
                            mWPM.setText("WPM: " + mGameEngine.calcRoundedWPM());
                            mGameEngine.setFinalWPM(mGameEngine.calcRoundedWPM());
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
}
