package com.github.kevin.recyclerview;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.github.kevin.library.RecyclerView;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setAdapter(new MyAdapter(this, 100));

    }
}


