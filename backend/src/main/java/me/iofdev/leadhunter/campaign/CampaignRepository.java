package me.iofdev.leadhunter.campaign;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class CampaignRepository {

    private static final String SELECT = """
            select c.id, c.slug, c.name, c.answers, c.search, c.created_at,
                   coalesce((select sum(r.cost_usd) from campaign_run r where r.campaign_id = c.id), 0) as total_cost_usd
            from campaign c
            """;

    private final JdbcClient jdbc;
    private final JsonMapper json;

    public CampaignRepository(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /** Inserts or updates by slug. Returns true when the campaign is new. */
    public boolean save(CampaignFile file) {
        return jdbc.sql("""
                        insert into campaign (slug, name, answers, search)
                        values (:slug, :name, cast(:answers as jsonb), cast(:search as jsonb))
                        on conflict (slug) do update
                            set name = excluded.name,
                                answers = excluded.answers,
                                search = excluded.search,
                                updated_at = now()
                        returning (xmax = 0) as inserted
                        """)
                .param("slug", file.slug())
                .param("name", file.name())
                .param("answers", json.writeValueAsString(file.answers()))
                .param("search", json.writeValueAsString(file.search()))
                .query(Boolean.class)
                .single();
    }

    public Optional<Campaign> findBySlug(String slug) {
        return jdbc.sql(SELECT + " where c.slug = :slug")
                .param("slug", slug)
                .query(this::map)
                .optional();
    }

    public List<Campaign> findAll() {
        return jdbc.sql(SELECT + " order by c.created_at").query(this::map).list();
    }

    private Campaign map(ResultSet rs, int row) throws SQLException {
        return new Campaign(
                rs.getLong("id"),
                rs.getString("slug"),
                rs.getString("name"),
                json.readValue(rs.getString("answers"), CampaignFile.Answers.class),
                json.readValue(rs.getString("search"), CampaignFile.Search.class),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getBigDecimal("total_cost_usd") == null ? BigDecimal.ZERO : rs.getBigDecimal("total_cost_usd"));
    }
}
