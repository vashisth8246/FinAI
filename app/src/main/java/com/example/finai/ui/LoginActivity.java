package com.example.finai.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.finai.databinding.ActivityLoginBinding;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        auth = FirebaseAuth.getInstance();

        binding.btnLogin.setOnClickListener(v -> login());
        binding.btnRegister.setOnClickListener(v -> register());
    }

    private void login() {
        String email = binding.email.getText().toString();
        String pass = binding.password.getText().toString();
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)) return;
        auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(r -> finish())
                .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void register() {
        String email = binding.email.getText().toString();
        String pass = binding.password.getText().toString();
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)) return;
        auth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener(r -> finish())
                .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}