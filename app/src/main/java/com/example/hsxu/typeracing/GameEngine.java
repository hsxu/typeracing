package com.example.hsxu.typeracing;

import android.graphics.Color;
import android.text.Spannable;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;

import org.w3c.dom.Text;

public class GameEngine {
    private TextView mStatus;
    private EditText mTypingBox;
    private TextView mMessageBox;
    private TextView mWPM;
    private Button mRestartButton;
    private Button mSendToLeaderboardsButton;

    private String[] mMessageToType;
    private int mWordIndex = 0;

    private Stopwatch mStopwatch = new Stopwatch();

    private int mFinalWPM = 0;

    private int mLineIndex = 0;
    private int mLineStartCharIndex;
    private int mLineEndCharIndex;
    private int mCurrentLineWordIndex = 0;

    private boolean mIsRestartedState = false;

    public GameEngine(EditText typingBox, TextView messageBox,
                      TextView WPM, Button restartButton,
                      String[] messageToType, int wordIndex,
                      TextView status,
                      Button sendToLeaderboardsButton) {
        mTypingBox = typingBox;
        mMessageBox = messageBox;
        mWPM = WPM;
        mRestartButton = restartButton;
        mMessageToType = messageToType;
        mWordIndex = wordIndex;
        mStatus = status;
        mSendToLeaderboardsButton = sendToLeaderboardsButton;

        mMessageBox.setMovementMethod(new ScrollingMovementMethod());
        //Highlight the first word.
        highlightWords(Color.parseColor("#FF8800"), mMessageToType[0].length());

        mSendToLeaderboardsButton.setVisibility(View.GONE);
    }

    public void runMainGameEngine(CharSequence charSequence) {
        if (mStopwatch.getIsStopWatchRunning() == false && isGameFinished() == false && mIsRestartedState == false) {
            mStopwatch.startStopwatch();
            updateStatus("Playing", "#669900");
        }
        mIsRestartedState = false;

        if (mWordIndex < mMessageToType.length) {
            //Only evaluated on last word
            if (doesCurrentTextMatchTargetText(charSequence.toString(), mMessageToType[mWordIndex])
                    && (mWordIndex + 1 == mMessageToType.length)) {
                updateTypingVariables();
                mWordIndex++;
            }
            //On all other words
            else if (doesCurrentTextMatchTargetText(charSequence.toString(), mMessageToType[mWordIndex] + " ")) {
                runScrollingLogic();
                updateTypingVariables();
                mWordIndex++;
            }
        }

        if (isGameFinished()) {
            mStopwatch.stopStopwatch();
            mRestartButton.setVisibility(View.VISIBLE);
            mSendToLeaderboardsButton.setVisibility(View.VISIBLE);
            mTypingBox.setVisibility(View.GONE);
            updateStatus("Finished", "#FF4444");

            //If the game is finished clear the highlighting.
            highlightWords(Color.BLACK, mMessageBox.getText().toString().length());
        }
    }

    private void updateStatus(String text, String color) {
        mStatus.setText(text);
        mStatus.setTextColor(Color.parseColor(color));
    }

    private void highlightWords(int color, int endingCharPosition) {
        Spannable spannable = new SpannableString(mMessageBox.getText().toString());
        spannable.setSpan(new ForegroundColorSpan(color), 0, endingCharPosition, 0); //for reset of the colors.
        mMessageBox.setText(spannable);
    }

    private void updateTypingVariables() {
        if (mWordIndex + 1 < mMessageToType.length) {
            int startingCharPosition = calcStartingCharPosition(mWordIndex, mMessageToType);
            int endingCharPosition = startingCharPosition + mMessageToType[mWordIndex + 1].length();

            //To highlight each word after the previous word is completed
            Spannable spannable = new SpannableString(mMessageBox.getText().toString());
            spannable.setSpan(new ForegroundColorSpan(Color.BLACK), 0, mMessageBox.getText().toString().length(), 0); //for reset of the colors.
            spannable.setSpan(new ForegroundColorSpan(Color.parseColor("#FF8800")), startingCharPosition, endingCharPosition , 0);
            mMessageBox.setText(spannable);
        }
        mTypingBox.setText("");
    }

    public void restartGame() {
        mIsRestartedState = true;

        mWordIndex = 0;
        mTypingBox.setText("");

        mTypingBox.setVisibility(View.VISIBLE);
        mRestartButton.setVisibility(View.GONE);
        mSendToLeaderboardsButton.setVisibility(View.GONE);

        //Reset WPM.
        mWPM.setText("WPM: 0");

        //Reset status.
        updateStatus("Ready", "#FFBB33");

        //Reset scrolling variables.
        mLineIndex = 0;
        mCurrentLineWordIndex = 0;
        mMessageBox.scrollTo(0,0);

        //Highlight the first word.
        highlightWords(Color.parseColor("#FF8800"), mMessageToType[0].length());
    }

    private void setStartAndEndIndex() {
        mLineStartCharIndex = mMessageBox.getLayout().getLineStart(mLineIndex);
        mLineEndCharIndex = mMessageBox.getLayout().getLineEnd(mLineIndex);
    }

    private void runScrollingLogic() {
        if (mLineIndex == 0) {
            setStartAndEndIndex();
        }

        String currentLine = mMessageBox.getText().toString().substring(mLineStartCharIndex, mLineEndCharIndex);
        String[] currentLineWords = currentLine.split(" ");

        //When the last word is spelled, scroll down 1 line
        if (mCurrentLineWordIndex == currentLineWords.length - 1) {
            //TODO fix scroll y-length
            mMessageBox.scrollBy(0, 70);

            mCurrentLineWordIndex = 0;
            mLineIndex++;

            if (mLineIndex != mMessageBox.getLineCount() - 1) {
                setStartAndEndIndex();
            }
        }

        if (currentLineWords[mCurrentLineWordIndex].equals(mMessageToType[mWordIndex])) {
            mCurrentLineWordIndex++;
        }
    }

    //The method to calculate where the starting char index of the word is
    private static int calcStartingCharPosition(int wordIndex, String[] messageToType) {
        int tempStartingCharPosition = 0;

        for (int i = 0; i <= wordIndex; i++) {
            tempStartingCharPosition += messageToType[i].length();
            tempStartingCharPosition += 1; //for the space.
        }
        return tempStartingCharPosition;
    }

    public int calcRoundedWPM() {
        int numTotalChars = 0;
        for (int i = 0; i < mWordIndex; i++) {
            numTotalChars += mMessageToType[i].length();
            numTotalChars += 1; //for each space!
        }

        double numWords = (double)(numTotalChars/5);
        double wpm = ((numWords)/mStopwatch.elapsedTime())*60;
        double roundedWPM = Math.round(wpm*10)/10;
        return (int)roundedWPM;
    }

    private boolean isGameFinished() {
        return mWordIndex == mMessageToType.length;
    }

    private boolean doesCurrentTextMatchTargetText(String currentText, String targetText) {
        return currentText.equals(targetText);
    }

    public int getWordIndex() {
        return mWordIndex;
    }

    public int getFinalWPM(){
        return mFinalWPM;
    }

    public void setFinalWPM(int finalWPM) {
        mFinalWPM = finalWPM;
    }
}
