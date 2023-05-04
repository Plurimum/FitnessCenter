package ru.itmo.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.itmo.model.Event;
import ru.itmo.model.FullEvent;
import ru.itmo.model.Type;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
public class EventRepository extends JdbcTemplate {
    private final RowMapper<FullEvent> eventMapper = (rs, a) ->
            new FullEvent(
                    rs.getString("name"),
                    rs.getInt("season_ticket_id"),
                    rs.getInt("version"),
                    rs.getTimestamp("created_at").toInstant(),
                    Type.valueOf(rs.getString("event_type")),
                    rs.getString("data")
            );

    public EventRepository(DataSource dataSource) {
        super(dataSource);
    }

    public void appendEvent(Event event) {
        update(
                "insert into events(season_ticket_id, version, event_type, data) values (?, ?, ?, ?);",
                event.seasonTicketId(),
                event.version(),
                event.eventType().name(),
                event.data()
        );
    }

    public List<FullEvent> eventBySeasonTicketId(int id) {
        return query(
                """
                        select name, season_ticket_id, e.version, e.created_at, e.event_type, e.data
                            from season_tickets st
                        inner join events e on st.id = e.season_ticket_id
                        where season_ticket_id = ?
                        order by e.version;
                        """,
                eventMapper,
                id
        );
    }

    public int countEvents(Instant from, Instant to) {
        return query(
                """
                        select count(*) from events
                        where created_at >= ? and created_at <= ? and event_type = 'START_VISIT';
                        """,
                (rs, a) -> rs.getInt("count"),
                Timestamp.from(from),
                Timestamp.from(to)
        ).get(0);
    }
}
