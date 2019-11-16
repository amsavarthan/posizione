package com.amsavarthan.posizione.ui.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import com.amsavarthan.posizione.R;

public class WelcomeScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome_screen);

        TextView textView=findViewById(R.id.text);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        Spanned htmlText= null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            htmlText = Html.fromHtml("By clicking the button below you accept our <a href='https://lvamsavarthan.github.io/lvstore/posizione_terms_and_conditions.html'>Terms of Service</a> and <a href='https://lvamsavarthan.github.io/lvstore/posizione_privacy_policy.html'>Privacy Policy</a>",Html.FROM_HTML_MODE_COMPACT);
        }else{
            htmlText = Html.fromHtml("By clicking the button below you accept our <a href='https://lvamsavarthan.github.io/lvstore/posizione_terms_and_conditions.html'>Terms of Service</a> and <a href='https://lvamsavarthan.github.io/lvstore/posizione_privacy_policy.html'>Privacy Policy</a>");
        }
        textView.setText(htmlText);

    }

    public void getStarted(View view) {

        startActivity(new Intent(this,PhoneAuthActivity.class));
        finish();

    }
}
