package ro.dsgmedia.cox.nfcoffee;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;

/**
 * Replaces the SimpleAdapter/ListView pair. Holds the same two lines per row
 * (name [count] and code name) and keeps the "selected" highlight used by the
 * delete flow.
 */
public class CardAdapter extends RecyclerView.Adapter<CardAdapter.CardViewHolder> {

    public interface OnCardAction {
        void onCardClick(int position);

        void onCardLongClick(int position);
    }

    private final List<CardEntry> items = new ArrayList<>();
    private final OnCardAction action;
    private int selectedPosition = RecyclerView.NO_POSITION;

    public CardAdapter(OnCardAction action) {
        this.action = action;
    }

    public void submit(List<CardEntry> newItems) {
        items.clear();
        items.addAll(newItems);
        selectedPosition = RecyclerView.NO_POSITION;
        notifyDataSetChanged();
    }

    public CardEntry getItem(int position) {
        return items.get(position);
    }

    public void setSelectedPosition(int position) {
        int previous = selectedPosition;
        selectedPosition = position;
        if (previous != RecyclerView.NO_POSITION) {
            notifyItemChanged(previous);
        }
        if (position != RecyclerView.NO_POSITION) {
            notifyItemChanged(position);
        }
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        CardEntry item = items.get(position);
        // Cards that came from the reader and were never edited have a NULL
        // UNAME, which used to render as the literal text "null".
        String displayName = item.userName;
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = holder.itemView.getContext().getString(R.string.unnamed_card);
        }
        holder.name.setText(holder.itemView.getContext()
                .getString(R.string.card_row, displayName, item.coffees));
        holder.codeName.setText(item.codeName);
        // Palette-aware selection tint; a theme attribute inside a
        // StateListDrawable fails to inflate, so it is resolved here instead.
        if (position == selectedPosition) {
            holder.itemView.setBackgroundColor(
                    MaterialColors.getColor(holder.itemView,
                            com.google.android.material.R.attr.colorSecondaryContainer));
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class CardViewHolder extends RecyclerView.ViewHolder {

        final TextView name;
        final TextView codeName;

        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.list_item_text_view);
            codeName = itemView.findViewById(R.id.list_item_subtext_view);

            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    action.onCardClick(position);
                }
            });
            itemView.setOnLongClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    action.onCardLongClick(position);
                }
                return true;
            });
        }
    }
}
