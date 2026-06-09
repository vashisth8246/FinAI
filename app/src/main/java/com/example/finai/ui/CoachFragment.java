package com.example.finai.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.finai.databinding.FragmentCoachBinding;

import java.util.List;

public class CoachFragment extends Fragment {

    private FragmentCoachBinding binding;
    private SpeechRecognizer speechRecognizer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCoachBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.micButton.setOnClickListener(v -> startVoice());
    }

    private void startVoice() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 201);
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            Toast.makeText(requireContext(), "Speech recognition not available on this device", Toast.LENGTH_LONG).show();
            return;
        }
        if (speechRecognizer == null) speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext());
        var intent = new android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { }
            @Override public void onBeginningOfSpeech() { }
            @Override public void onRmsChanged(float rmsdB) { }
            @Override public void onBufferReceived(byte[] buffer) { }
            @Override public void onEndOfSpeech() { }
            @Override public void onError(int error) { Toast.makeText(requireContext(), "Mic error: "+error, Toast.LENGTH_SHORT).show(); }
            @Override public void onResults(Bundle results) {
                List<String> list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (list != null && !list.isEmpty()) {
                    binding.queryText.setText(list.get(0));
                    // TODO: Send to Gemini and show response
                }
            }
            @Override public void onPartialResults(Bundle partialResults) { }
            @Override public void onEvent(int eventType, Bundle params) { }
        });
        speechRecognizer.startListening(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 201 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startVoice();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (speechRecognizer != null) speechRecognizer.stopListening();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (speechRecognizer != null) speechRecognizer.destroy();
        binding = null;
    }
}
