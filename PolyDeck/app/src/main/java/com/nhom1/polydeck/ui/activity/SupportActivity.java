package com.nhom1.polydeck.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.utils.SessionManager;

public class SupportActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support);

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> onBackPressed());

        EditText etName = findViewById(R.id.et_name);
        EditText etEmail = findViewById(R.id.et_email);
        EditText etMessage = findViewById(R.id.et_message);
        Button btnSend = findViewById(R.id.btn_send);

        SessionManager sm = new SessionManager(this);
        if (sm.getUserData() != null) {
            etName.setText(sm.getUserData().getHoTen());
            etEmail.setText(sm.getUserData().getEmail());
        }

        btnSend.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String msg = etMessage.getText().toString().trim();
            if (msg.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập nội dung", Toast.LENGTH_SHORT).show();
                return;
            }
            String body = "Tên: " + name + "%0D%0AEmail: " + email + "%0D%0A%0D%0ANội dung:%0D%0A" + Uri.encode(msg);
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:support@polydeck.com?subject=Yeu%20cau%20ho%20tro&body=" + body));
            startActivity(Intent.createChooser(intent, "Gửi email bằng..."));
        });
    }
}


