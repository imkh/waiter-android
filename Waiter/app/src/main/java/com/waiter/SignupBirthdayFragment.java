package com.waiter;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import com.securepreferences.SecurePreferences;
import com.waiter.custom.CustomTextInputLayout;
import com.waiter.models.RequestSignup;
import com.waiter.models.ResponseSignup;
import com.waiter.network.ClientGenerator;
import com.waiter.network.WaiterClient;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import agency.tango.materialintroscreen.SlideFragment;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignupBirthdayFragment extends SlideFragment implements View.OnClickListener {

    private static final String TAG = "SignupBirthdayFragment";

    private static final long MINIMUM_AGE = 568036800000L; // 18 years old (1000 * 60 * 60 * 24 * 365.25 * 18)

    private ScrollView mScrollView;
    private TextView mWelcomeUser;
    private EditText mInputBirthday;
    private CustomTextInputLayout mInputLayoutBirthday;
    private DatePickerDialog mDatePickerDialog;
    private Button mSignupButton;
    private ProgressDialog mProgressDialog;

    private Calendar mCalendar;
    private DatePickerDialog.OnDateSetListener date;

    private boolean signedUp = false;
    private boolean firstAttempt = true;

    private IntroActivity introActivity;

    private WaiterClient waiterClient;
    private RequestSignup requestSignup;

    private View view;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_signup_birthday, container, false);

        mProgressDialog = new ProgressDialog(getContext(), R.style.AppTheme_Dark_Dialog);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMessage(getString(R.string.signing_up));

        mInputBirthday = (EditText) view.findViewById(R.id.input_birthday);
        mInputBirthday.setOnClickListener(this);
        mInputLayoutBirthday = (CustomTextInputLayout) view.findViewById(R.id.input_layout_birthday);
        mSignupButton = (Button) view.findViewById(R.id.signup_btn);
        mSignupButton.setOnClickListener(this);

        mScrollView = (ScrollView) view.findViewById(R.id.scroll_view);
        mWelcomeUser = (TextView) view.findViewById(R.id.welcome_user);

        introActivity = ((IntroActivity)getActivity());

        waiterClient = ClientGenerator.createClient(WaiterClient.class);
        requestSignup = new RequestSignup();

        setupDatePicker();

        return view;
    }

    private void setupDatePicker() {
        mCalendar = Calendar.getInstance();
        date = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                mCalendar.set(Calendar.YEAR, year);
                mCalendar.set(Calendar.MONTH, monthOfYear);
                mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateLabel();
            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mDatePickerDialog = new DatePickerDialog(getContext(), R.style.PickerDialogCustom, date, mCalendar.get(Calendar.YEAR) - 21, mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH));
            mDatePickerDialog.setTitle(getString(R.string.datepicker_title));
        } else {
            mDatePickerDialog = new DatePickerDialog(getContext(), date, mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH));
        }
        mDatePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis() - MINIMUM_AGE);
    }

    private void updateLabel() {
        String myFormat = "MM/dd/yyyy"; //In which you need put here
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
        mInputBirthday.setText(sdf.format(mCalendar.getTime()));
    }

    @Override
    public void onStart() {
        super.onStart();
        mInputBirthday.requestFocus();
    }

    @Override
    public int backgroundColor() {
        return R.color.colorPrimary;
    }

    @Override
    public int buttonsColor() {
        return R.color.colorPrimaryDark;
    }

    public void disableInput() {
        mInputBirthday.setEnabled(false);
        mSignupButton.setEnabled(false);
        introActivity.disableInput();
    }

    @Override
    public boolean canMoveFurther() {
        return signedUp;
    }

    @Override
    public String cantMoveFurtherErrorMessage() {
        return getString(R.string.please_sign_up);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.input_birthday:
                onClickInputBirthday();
                break;
            case R.id.signup_btn:
                onClickSignupButton();
                break;
            default:
                break;
        }
    }

    private void onClickInputBirthday() {
        mDatePickerDialog.show();
    }

    private void onClickSignupButton() {
        Utils.hideKeyboard(getActivity());

        if (firstAttempt) {
            mInputBirthday.addTextChangedListener(new MyTextWatcher(mInputBirthday));
            firstAttempt = false;
        }

        boolean validForm = submitForm();
        if (validForm) {
            mInputBirthday.clearFocus();
            signup();
        }
    }

    private void signup() {
        mProgressDialog.show();

        requestSignup.setFirstname(introActivity.getFirstName());
        requestSignup.setLastname(introActivity.getLastName());
        requestSignup.setEmail(introActivity.getEmailAddress());
        requestSignup.setPassword(introActivity.getPassword());
        requestSignup.setBirthday(mInputBirthday.getText().toString().trim());
        requestSignup.setType(0);
        Log.d(TAG, "signup: requestSignup = " + requestSignup);
        Call<ResponseSignup> call = waiterClient.signup(requestSignup);

        call.enqueue(new Callback<ResponseSignup>() {
            @Override
            public void onResponse(@NonNull Call<ResponseSignup> call, @NonNull Response<ResponseSignup> response) {
                mProgressDialog.dismiss();
                if (response.isSuccessful()) {
                    ResponseSignup responseSignup = response.body();
                    if (responseSignup != null) {
                        signedUp = true;
                        Log.d(TAG, "onResponse: user = " + responseSignup.getData().getUser());
                        mWelcomeUser.setText(getString(R.string.hi_user_welcome, responseSignup.getData().getUser().getId()));
                        mScrollView.setVisibility(View.GONE);
                        mWelcomeUser.setVisibility(View.VISIBLE);

                        disableInput();
                        introActivity.setSignedUp(true);

                        SharedPreferences prefs = new SecurePreferences(getContext());
                        prefs.edit().putBoolean("is_logged_in", true).apply();
//                        prefs.edit().putString("token", responseSignup.getData().getUser().getToken()).apply();
                    } else {
                        showErrorSnackbar(getString(R.string.response_body_null));
                    }
                } else {
                    ResponseBody errorBody = response.errorBody();
                    if (errorBody != null) {
//                        showErrorSnackbar(getString(R.string.email_already_used));
                    } else {
                        showErrorSnackbar(getString(R.string.response_error_body_null));
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseSignup> call, @NonNull Throwable t) {
                mProgressDialog.dismiss();
                showErrorSnackbar(t.getLocalizedMessage());
            }
        });
    }

    private boolean submitForm() {
        boolean validBirthday = true;

        validBirthday = validateBirthday();

        return validBirthday;
    }

    private boolean validateBirthday() {
        String birthday = mInputBirthday.getText().toString();

        if (!isValidBirthday(birthday)) {
            if (!mInputLayoutBirthday.isErrorEnabled()) {
                mInputLayoutBirthday.setHelperTextEnabled(true);
            }
            mInputLayoutBirthday.setError(getString(R.string.birthday_instructions));
            return false;
        }
        mInputLayoutBirthday.setErrorEnabled(false);
        mInputLayoutBirthday.setHelperTextEnabled(false);

        return true;
    }

    private boolean isValidBirthday(String birthday) {
        return !TextUtils.isEmpty(birthday);
    }

    private void requestFocus(View view) {
        if (view.requestFocus()) {
            ((Activity) getContext()).getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    private void showErrorSnackbar(String message) {
        if (view != null) {
            Snackbar.make(view, message, Snackbar.LENGTH_SHORT).setCallback(new Snackbar.Callback() {
                @Override
                public void onDismissed(Snackbar snackbar, int event) {
                    introActivity.getNavigationView().setTranslationY(0f);
                    super.onDismissed(snackbar, event);
                }
            }).show();
        }
    }

    private class MyTextWatcher implements TextWatcher {

        private View view;

        private MyTextWatcher(View view) {
            this.view = view;
        }

        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

        public void afterTextChanged(Editable editable) {
            switch (view.getId()) {
                case R.id.input_birthday:
                    validateBirthday();
                    break;
            }
        }
    }
}