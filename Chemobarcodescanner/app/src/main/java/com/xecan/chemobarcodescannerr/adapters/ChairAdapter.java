package com.xecan.chemobarcodescannerr.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.xecan.chemobarcodescannerr.ChairInfo;
import com.xecan.chemobarcodescannerr.R;


import java.util.ArrayList;

public class ChairAdapter extends RecyclerView.Adapter<ChairAdapter.ViewHolder> {
    Context context;
    ArrayList<ChairInfo> chairInfos;

    public ChairAdapter(Context context, ArrayList<ChairInfo> jsonObjects) {
        this.context = context;
        this.chairInfos = jsonObjects;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_chair,parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChairInfo chairInfo = chairInfos.get(position);
        holder.tvChair.setText("Patient Name:"+chairInfo.getPatName()+"       "+"Description:"+chairInfo.getDesc());
        if(chairInfo.getType().equalsIgnoreCase("G")){
            holder.tvChair.setTextColor(Color.GREEN);
        }else {
            holder.tvChair.setTextColor(Color.BLACK);
        }
    }

    @Override
    public int getItemCount() {
        return chairInfos.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvChair;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChair = itemView.findViewById(R.id.tvChairInfo);
        }
    }
    public void update(ArrayList<ChairInfo> chairInfos){
        this.chairInfos.clear();
        this.chairInfos.addAll(chairInfos);
        this.notifyDataSetChanged();
    }
}
