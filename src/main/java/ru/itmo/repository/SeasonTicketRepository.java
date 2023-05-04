package ru.itmo.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.itmo.model.Pair;

import javax.sql.DataSource;
import java.util.List;

@Repository
public class SeasonTicketRepository extends JdbcTemplate {
    public SeasonTicketRepository(DataSource dataSource) {
        super(dataSource);
    }

    public Pair<Integer, Integer> registerSeasonTicket(String nameUser) {
        return query(
                """
                        insert into season_tickets(name) 
                        values (?) returning id, version;
                        """,
                (rs, a) -> new Pair<>(rs.getInt("id"), rs.getInt("version")),
                nameUser
        ).get(0);
    }

    public int getNextVersionBySeasonTicketId(int seasonTicketId) {
        return query(
                """
                        update season_tickets 
                        set version = version + 1 
                        where id = ? returning version;
                        """,
                (rs, a) -> rs.getInt("version"),
                seasonTicketId
        ).get(0);
    }

    public List<Integer> findAll() {
        return query(
                "select id from season_tickets;",
                (rs, a) -> rs.getInt("id")
        );
    }
}
