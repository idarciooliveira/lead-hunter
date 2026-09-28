package me.iofdev.leadhunter.place;

import java.util.Optional;

import me.iofdev.leadhunter.maps.ScrapedPlace;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PlaceRepository {

    private final JdbcClient jdbc;

    public PlaceRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Inserts or refreshes a place by its Google place ID and returns the row id. */
    public long upsert(ScrapedPlace place, Optional<PhoneNumber> phone, WebsiteKind websiteKind) {
        return jdbc.sql("""
                        insert into place (google_place_id, name, category, categories, address, neighborhood, city,
                                           phone_raw, phone_e164, phone_mobile, website, website_kind, rating,
                                           reviews_count, latitude, longitude, maps_url, permanently_closed,
                                           temporarily_closed, raw)
                        values (:googlePlaceId, :name, :category, :categories, :address, :neighborhood, :city,
                                :phoneRaw, :phoneE164, :phoneMobile, :website, :websiteKind, :rating,
                                :reviewsCount, :latitude, :longitude, :mapsUrl, :permanentlyClosed,
                                :temporarilyClosed, cast(:raw as jsonb))
                        on conflict (google_place_id) do update
                            set name = excluded.name,
                                category = excluded.category,
                                categories = excluded.categories,
                                address = excluded.address,
                                neighborhood = excluded.neighborhood,
                                city = excluded.city,
                                phone_raw = excluded.phone_raw,
                                phone_e164 = excluded.phone_e164,
                                phone_mobile = excluded.phone_mobile,
                                website = excluded.website,
                                website_kind = excluded.website_kind,
                                rating = excluded.rating,
                                reviews_count = excluded.reviews_count,
                                latitude = excluded.latitude,
                                longitude = excluded.longitude,
                                maps_url = excluded.maps_url,
                                permanently_closed = excluded.permanently_closed,
                                temporarily_closed = excluded.temporarily_closed,
                                raw = excluded.raw,
                                last_scraped_at = now()
                        returning id
                        """)
                .param("googlePlaceId", place.googlePlaceId())
                .param("name", place.name())
                .param("category", place.category())
                .param("categories", place.categories().toArray(String[]::new))
                .param("address", place.address())
                .param("neighborhood", place.neighborhood())
                .param("city", place.city())
                .param("phoneRaw", place.phone())
                .param("phoneE164", phone.map(PhoneNumber::e164).orElse(null))
                .param("phoneMobile", phone.map(PhoneNumber::mobile).orElse(false))
                .param("website", place.website())
                .param("websiteKind", websiteKind.name())
                .param("rating", place.rating())
                .param("reviewsCount", place.reviewsCount())
                .param("latitude", place.latitude())
                .param("longitude", place.longitude())
                .param("mapsUrl", place.mapsUrl())
                .param("permanentlyClosed", place.permanentlyClosed())
                .param("temporarilyClosed", place.temporarilyClosed())
                .param("raw", place.rawJson())
                .query(Long.class)
                .single();
    }
}
