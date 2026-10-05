package me.iofdev.leadhunter.pipeline;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Proof that a job's process is alive (ADR 0036): the session advisory lock
 * on the job id, held on its own connection until the job closes. If the
 * process dies, Postgres ends the session and frees the lock, which is how
 * {@link RunRepository#failAbandoned} tells a dead job from a live one.
 */
public final class JobLease implements AutoCloseable {

    private final long jobId;
    private final Connection connection;

    JobLease(long jobId, Connection connection) {
        this.jobId = jobId;
        this.connection = connection;
    }

    public long jobId() {
        return jobId;
    }

    /** Frees the lock and hands the connection back to the pool. */
    @Override
    public void close() {
        try (connection; PreparedStatement unlock = connection.prepareStatement(RunRepository.UNLOCK)) {
            unlock.setLong(1, jobId);
            unlock.execute();
        } catch (SQLException e) {
            // A broken connection is dropped by the pool; Postgres frees the lock with the session.
        }
    }
}
