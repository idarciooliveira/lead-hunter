# 0005. Get Google Maps data through the Apify scraper instead of the Places API

- Date: 2026-09-28
- Status: Accepted

## Context

The official Google Places API returns at most 60 results per text search, so covering a city means querying a grid of small cells. Phone, website, rating, and reviews sit in its pricier field tiers. Its terms also restrict storing most returned fields beyond the place ID.

Scraping Google Maps directly breaks Google's terms of service. Writing our own Playwright scraper means fixing selectors every time Google changes the Maps page.

## Decision

We use the Apify actor `compass/crawler-google-places`, paid per result. We start runs through the Apify REST API, poll until they finish, and read the dataset. Its input fields include `searchStringsArray`, `locationQuery`, and `maxCrawledPlacesPerSearch`. Its output includes `placeId`, `title`, `phone`, `phoneUnformatted`, `website`, `totalScore`, `reviewsCount`, and `categoryName`.

The owner accepts the terms-of-service gray area of using scraped Google data.

We store the raw item next to the parsed columns, so we can extract new fields later without paying for another scrape.

## Consequences

Cheaper per place than the Places API with details. One call returns reviews and social links when we ask for them. We depend on a third party that can change prices or output fields, so the parser reads fields leniently and tests cover it with a fixture. The Apify token lives in the `APIFY_TOKEN` environment variable.
