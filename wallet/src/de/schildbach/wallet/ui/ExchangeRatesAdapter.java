/*
 * Copyright the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.schildbach.wallet.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.utils.ExchangeRate;
import org.bitcoinj.utils.Fiat;

import de.schildbach.wallet.Constants;
import de.schildbach.wallet.R;
import de.schildbach.wallet.exchangerate.ExchangeRateEntry;
import de.schildbach.wallet.service.BlockchainState;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

/**
 * @author Andreas Schildbach
 */
public class ExchangeRatesAdapter extends ListAdapter<ExchangeRatesAdapter.ListItem, ExchangeRatesAdapter.ViewHolder> {
    public static List<ListItem> buildListItems(final List<ExchangeRateEntry> exchangeRates, final Coin balance,
                                                final BlockchainState blockchainState, final String defaultCurrency,
                                                final Coin rateBase) {
        final List<ListItem> items = new ArrayList<>(exchangeRates.size());
        for (final ExchangeRateEntry exchangeRate : exchangeRates) {
            final ExchangeRate rate = exchangeRate.exchangeRate();
            final Fiat baseRateAsFiat = rate.coinToFiat(rateBase);
            final int baseRateMinDecimals = !rateBase.isLessThan(Coin.COIN) ? 2 : 4;
            final Fiat balanceAsFiat = balance != null && (blockchainState == null || !blockchainState.replaying)
                    ? rate.coinToFiat(balance) : null;
            final String currencyCode = rate.fiat.currencyCode;
            final boolean isDefaultCurrency = currencyCode.equals(defaultCurrency);
            items.add(new ListItem(exchangeRate.getId(), currencyCode, baseRateAsFiat, baseRateMinDecimals,
                    balanceAsFiat, isDefaultCurrency));
        }
        return items;
    }

    public static class ListItem {
        public final long id;
        public final String currencyCode;
        public final Fiat baseRateAsFiat;
        public final int baseRateMinDecimals;
        public final Fiat balanceAsFiat;
        public final boolean isSelected;

        public ListItem(final long id, final String currencyCode, final Fiat baseRateAsFiat,
                        final int baseRateMinDecimals, final Fiat balanceAsFiat, final boolean isSelected) {
            this.id = id;
            this.currencyCode = currencyCode;
            this.baseRateAsFiat = baseRateAsFiat;
            this.baseRateMinDecimals = baseRateMinDecimals;
            this.balanceAsFiat = balanceAsFiat;
            this.isSelected = isSelected;
        }
    }

    private final LayoutInflater inflater;
    @Nullable
    private final OnClickListener onClickListener;

    public ExchangeRatesAdapter(final Context context, final @Nullable OnClickListener onClickListener) {
        super(new DiffUtil.ItemCallback<ListItem>() {
            @Override
            public boolean areItemsTheSame(final ListItem oldItem, final ListItem newItem) {
                return oldItem.currencyCode.equals(newItem.currencyCode);
            }

            @Override
            public boolean areContentsTheSame(final ListItem oldItem, final ListItem newItem) {
                if (!Objects.equals(oldItem.baseRateAsFiat, newItem.baseRateAsFiat))
                    return false;
                if (!Objects.equals(oldItem.baseRateMinDecimals, newItem.baseRateMinDecimals))
                    return false;
                if (!Objects.equals(oldItem.balanceAsFiat, newItem.balanceAsFiat))
                    return false;
                if (!Objects.equals(oldItem.isSelected, newItem.isSelected))
                    return false;
                return true;
            }
        });

        this.inflater = LayoutInflater.from(context);
        this.onClickListener = onClickListener;

        setHasStableIds(true);
    }

    @Override
    public long getItemId(final int position) {
        final ListItem listItem = getItem(position);
        return listItem.id;
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        return new ViewHolder(inflater.inflate(R.layout.exchange_rate_row, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final ListItem listItem = getItem(position);
        holder.itemView.setBackgroundResource(listItem.isSelected ? R.color.bg_level3 : R.color.bg_level2);
        holder.defaultView.setVisibility(listItem.isSelected ? View.VISIBLE : View.INVISIBLE);
        holder.currencyCodeView.setText(listItem.currencyCode);
        holder.rateView.setFormat(Constants.LOCAL_FORMAT.minDecimals(listItem.baseRateMinDecimals));
        holder.rateView.setAmount(listItem.baseRateAsFiat);
        holder.walletView.setFormat(Constants.LOCAL_FORMAT);
        if (listItem.balanceAsFiat != null) {
            holder.walletView.setAmount(listItem.balanceAsFiat);
            holder.walletView.setStrikeThru(!Constants.NETWORK_PARAMETERS.getId().equals(NetworkParameters.ID_MAINNET));
        } else {
            holder.walletView.setText("n/a");
            holder.walletView.setStrikeThru(false);
        }

        final OnClickListener onClickListener = this.onClickListener;
        if (onClickListener != null) {
            holder.menuView.setOnClickListener(v -> onClickListener.onExchangeRateMenuClick(v, listItem.currencyCode));
        }
    }

    public interface OnClickListener {
        void onExchangeRateMenuClick(View view, String currencyCode);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final View defaultView;
        private final TextView currencyCodeView;
        private final CurrencyTextView rateView;
        private final CurrencyTextView walletView;
        private final ImageButton menuView;

        public ViewHolder(final View itemView) {
            super(itemView);
            defaultView = itemView.findViewById(R.id.exchange_rate_row_default);
            currencyCodeView = itemView.findViewById(R.id.exchange_rate_row_currency_code);
            rateView = itemView.findViewById(R.id.exchange_rate_row_rate);
            walletView = itemView.findViewById(R.id.exchange_rate_row_balance);
            menuView = itemView.findViewById(R.id.exchange_rate_row_menu);
        }
    }
}
