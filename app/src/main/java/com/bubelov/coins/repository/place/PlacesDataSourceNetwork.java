package com.bubelov.coins.repository.place;

import com.bubelov.coins.api.coins.CoinsApi;
import com.bubelov.coins.api.coins.PlaceParams;
import com.bubelov.coins.model.Place;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Response;
import timber.log.Timber;

/**
 * @author Igor Bubelov
 */

@Singleton
public class PlacesDataSourceNetwork {
    private final CoinsApi api;

    @Inject
    PlacesDataSourceNetwork(CoinsApi api) {
        this.api = api;
    }

    public Place getPlace(long id) {
        try {
            return api.getPlace(id).execute().body();
        } catch (Exception e) {
            Timber.e(e, "Couldn't load place");
            return null;
        }
    }

    public Place addPlace(Place place, String authToken) {
        try {
            return api.addPlace(authToken, new PlaceParams(place)).execute().body();
        } catch (Exception e) {
            Timber.e(e, "Couldn't add place");
            return null;
        }
    }

    public Place updatePlace(Place place, String authToken) {
        try {
            return api.updatePlace(place.id(), authToken, new PlaceParams(place)).execute().body();
        } catch (Exception e) {
            Timber.e(e, "Couldn't update place");
            return null;
        }
    }

    public List<Place> getPlaces(Date updatedAfter) throws IOException {
        Response<List<Place>> response = api.getPlaces(updatedAfter, Integer.MAX_VALUE).execute();

        if (response.isSuccessful()) {
            return response.body();
        } else {
            throw new IOException("Couldn't fetch places");
        }
    }
}