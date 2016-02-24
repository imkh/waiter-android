package com.waiter.waiterpoc;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.waiter.waiterpoc.models.RegisterAttempt;
import com.waiter.waiterpoc.network.ServiceGenerator;
import com.waiter.waiterpoc.network.WaiterService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignupActivity extends AppCompatActivity {

    // UI References
    private EditText mFirstName;
    private EditText mLastName;
    private EditText mEmailText;
    private EditText mPasswordText;
    private EditText mRepeatPasswordText;
    private TextView mSignInLink;
    private Button mRegisterButton;
    private AlertDialog.Builder builder;
    private AlertDialog dialog;

    // Initial variables
    //private SharedPreferences sp;
    private static boolean connected = false;

    public static boolean isConnected() {
        return connected;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Set initial variables
        //sp = PreferenceManager.getDefaultSharedPreferences(MainActivity.getsContext());

        // Set up the register form
        mFirstName = (EditText) findViewById(R.id.input_first_name);
        mFirstName.requestFocus();
        mLastName = (EditText) findViewById(R.id.input_last_name);
        mEmailText = (EditText) findViewById(R.id.input_email);
        mPasswordText = (EditText) findViewById(R.id.input_password);
        mRepeatPasswordText = (EditText) findViewById(R.id.input_repeat_password);

        mRegisterButton = (Button) findViewById(R.id.btn_signup);
        mRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signup();
            }
        });

        mSignInLink = (TextView) findViewById(R.id.link_sign_in);
        mSignInLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Finish the registration screen and return to the Login activity
                finish();
            }
        });

        // Set up the AlertDialog
        builder = new AlertDialog.Builder(SignupActivity.this);
        builder.setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setMessage(R.string.signup_failed_message).setTitle(R.string.signup_failed_title);
        dialog = builder.create();
    }

    public void signup() {
        Log.d("SignupActivity", "Signup");

        if (!validate()) {
            //onSignupFailed();
            return ;
        }

        mRegisterButton.setEnabled(false);

        final ProgressDialog progressDialog = new ProgressDialog(SignupActivity.this, R.style.AppTheme_Dark_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Creating account...");
        progressDialog.show();

        final String firstName = mFirstName.getText().toString();
        final String lastName = mLastName.getText().toString();
        final String email = mEmailText.getText().toString();
        final String password = mPasswordText.getText().toString();
        final String repeatPassword = mRepeatPasswordText.getText().toString();

        WaiterService service = ServiceGenerator.createService(WaiterService.class);

        RegisterAttempt registerAttempt = new RegisterAttempt(firstName, lastName, email, password, repeatPassword);

        Call<RegisterAttempt> call = service.basicRegister(registerAttempt);

        call.enqueue(new Callback<RegisterAttempt>() {
            @Override
            public void onResponse(Call<RegisterAttempt> call, Response<RegisterAttempt> response) {
                if (response.isSuccess()) {
                    Log.d("Success", "Return: " + response.message() + " - Raw: " + response.raw().toString());
                    /*
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("email", email);
                    editor.putString("password", password);
                    editor.commit();
                    */
                    progressDialog.dismiss();

                    onSignupSuccess();
                } else {
                    Log.d("Failure", "Return: " + response.message() + " - Raw: " + response.raw().toString());
                    progressDialog.dismiss();
                    onSignupFailed();
                }
            }

            @Override
            public void onFailure(Call<RegisterAttempt> call, Throwable t) {
                Log.d("Error", t.getMessage());
                progressDialog.dismiss();
                onSignupFailed();
            }
        });
    }

    public void onSignupSuccess() {
        Log.d("onSignupSuccess", "Function onSignupSuccess");
        mRegisterButton.setEnabled(true);
        setResult(RESULT_OK, null);
        finish();
    }

    public void onSignupFailed() {
        Log.d("onSignupFailed", "Function onSignupFailed");
        //Toast.makeText(getBaseContext(), "Registration failed", Toast.LENGTH_LONG).show();
        mRegisterButton.setEnabled(true);
        dialog.show();
    }

    public boolean validate() {
        boolean valid = true;

        String firstName = mFirstName.getText().toString();
        String lastName = mLastName.getText().toString();
        String email = mEmailText.getText().toString();
        String password = mPasswordText.getText().toString();
        String repeatPassword = mRepeatPasswordText.getText().toString();

        if (firstName.isEmpty()) {
            mFirstName.setError(getString(R.string.error_field_required));
            valid = false;
        } else {
            mFirstName.setError(null);
        }

        if (lastName.isEmpty()) {
            mLastName.setError(getString(R.string.error_field_required));
            valid = false;
        } else {
            mLastName.setError(null);
        }

        if (email.isEmpty()) {
            mEmailText.setError(getString(R.string.error_field_required));
            valid = false;
        } else {
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                mEmailText.setError(getString(R.string.error_invalid_email));
                valid = false;
            } else {
                mEmailText.setError(null);
            }
        }

        if (password.isEmpty()) {
            mPasswordText.setError(getString(R.string.error_field_required));
            valid = false;
        } else {
            if (password.length() < 6 || password.length() > 20) {
                mPasswordText.setError(getString(R.string.error_invalid_password));
                valid = false;
            } else {
                mPasswordText.setError(null);
            }
        }

        if (repeatPassword.isEmpty()) {
            mRepeatPasswordText.setError(getString(R.string.error_field_required));
            valid = false;
        } else {
            if (!password.equals(repeatPassword)) {
                mRepeatPasswordText.setError(getString(R.string.no_match));
                valid = false;
            } else {
                mRepeatPasswordText.setError(null);
            }
        }

        return valid;
    }
}