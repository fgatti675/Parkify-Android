package com.bahpps.cahue.cars;


import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.bahpps.cahue.R;
import com.bahpps.cahue.util.DividerItemDecoration;

import java.util.List;

/**
 * This Activity appears as a dialog. It lists any paired devices and devices detected in the area after discovery. When
 * a device is chosen by the user, the MAC address of the device is sent back to the parent Activity in the result
 * Intent.
 */
public class CarManagerFragment extends Fragment {

    // Debugging
    private static final String TAG = CarManagerFragment.class.getSimpleName();

    // Member fields
    private CarDatabase carDatabase;

    private List<Car> cars;

    private RecyclerViewCarsAdapter adapter;


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment CarDetailsFragment.
     */
    public static CarManagerFragment newInstance() {
        CarManagerFragment fragment = new CarManagerFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_car_manager, container, false);

        /**
         * Device selection
         */


        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.cardList);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);

        cars = carDatabase.retrieveCars(false);

        adapter = new RecyclerViewCarsAdapter();

        recyclerView.setAdapter(adapter);

        RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration();
        recyclerView.addItemDecoration(itemDecoration);

        // this call is actually only necessary with custom ItemAnimators
        recyclerView.setItemAnimator(new DefaultItemAnimator());

//        recyclerView.addOnItemTouchListener(this);

        return view;
    }


    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        carDatabase = new CarDatabase(getActivity());
    }


    @Override
    public void onPause() {
        super.onPause();
    }


    public final static class CarViewHolder extends RecyclerView.ViewHolder {
        private TextView name;
        private TextView time;

        public CarViewHolder(View itemView) {
            super(itemView);

            name = (TextView) itemView.findViewById(R.id.name);
            time = (TextView) itemView.findViewById(R.id.time);
        }
    }

    public class RecyclerViewCarsAdapter extends
            RecyclerView.Adapter<RecyclerView.ViewHolder> {

        public static final int CAR_TYPE = 1;
        public static final int BT_DEVICES_TYPE = 2;

        @Override
        public int getItemViewType(int position) {

           if (position < cars.size())
                return CAR_TYPE;
            else
                return BT_DEVICES_TYPE;

        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            if (viewType == CAR_TYPE) {
                View itemView = LayoutInflater.from(viewGroup.getContext()).
                        inflate(R.layout.fragment_car_details,
                                viewGroup,
                                false);

                return new CarViewHolder(itemView);
            }

            else if (viewType == BT_DEVICES_TYPE) {
                View itemView = LayoutInflater.from(viewGroup.getContext()).
                        inflate(R.layout.fragment_device_selection,
                                viewGroup,
                                false);

                return new SimpleViewHolder(itemView);
            }

            throw new IllegalStateException("New type added to the recycler view but no view holder associated");
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {

            if (position < cars.size()) {

                CarViewHolder carViewHolder = (CarViewHolder) viewHolder;

                Car car = cars.get(position);

                carViewHolder.name.setText(car.name);
                if (car.time != null)
                    carViewHolder.time.setText(DateUtils.getRelativeTimeSpanString(car.time.getTime()));
            }
        }

        @Override
        public int getItemCount() {
            return cars.size() + 1;
        }
    }


    private void addCar(Car car) {
        cars.add(car);
        adapter.notifyDataSetChanged();
    }

    public interface DeviceSelectionLoadingListener {

        public void devicesBeingLoaded(boolean loading);

    }

    private class SimpleViewHolder extends RecyclerView.ViewHolder {
        public SimpleViewHolder(View view) {
            super(view);
        }
    }
}
