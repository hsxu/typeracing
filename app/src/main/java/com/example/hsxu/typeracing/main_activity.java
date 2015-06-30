package com.example.hsxu.typeracing;

import android.content.Intent;
import android.os.Handler;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.games.Games;
import com.google.example.games.basegameutils.BaseGameActivity;


public class main_activity extends BaseGameActivity implements
        View.OnClickListener {
    private static String LEADERBOARD_ID = "CgkIlKKJyP4WEAIQAg";

    private Handler mHandler = new Handler();

    private com.google.android.gms.common.SignInButton mSignInButton;
    private Button mSignOutButton;
    private Button mSingleplayerButton;
    private Button mMultiplayerButton;
    private Button mLeaderboardsButton;

    final static int[] requiresListeners = {
            R.id.sign_out_button,
            R.id.singleplayer_button,
            R.id.multiplayer_button,
            R.id.leaderboards_button
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getActionBar().hide();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_activity);

        mSignInButton = (com.google.android.gms.common.SignInButton)findViewById(R.id.sign_in_button);
        mSignOutButton = (Button)findViewById(R.id.sign_out_button);
        mSingleplayerButton = (Button)findViewById(R.id.singleplayer_button);
        mMultiplayerButton = (Button)findViewById(R.id.multiplayer_button);
        mLeaderboardsButton = (Button)findViewById(R.id.leaderboards_button);

        findViewById(R.id.sign_in_button).setOnClickListener(this);
        for (int id : requiresListeners) {
            findViewById(id).setOnClickListener(this);
            findViewById(id).setOnTouchListener(new GoldBorderOnTouchListener((Button)findViewById(id)));
        }

        getGameHelper().setMaxAutoSignInAttempts(0);
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

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
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
            case R.id.singleplayer_button:  {
                Intent intent = new Intent(this, singleplayer_activity.class);
                startActivity(intent);
                break;
            }
            case R.id.multiplayer_button: {
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                break;
            }
            case R.id.leaderboards_button: {
                if (getApiClient().isConnected()) {
                    startActivityForResult(Games.Leaderboards.getLeaderboardIntent(getApiClient(),
                            LEADERBOARD_ID), 100);
                } else {
                    Toast toast = Toast.makeText(this, "Please sign in to view leaderboards.", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
                break;
            }
        }
    }
}
