package com.groute.groute_server.common.transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 트랜잭션 커밋 이후에 작업을 실행하는 유틸리티.
 *
 * <p>활성 트랜잭션이 있으면 커밋 후 실행하고, 없으면(테스트 등) 즉시 실행한다. S3 삭제처럼 롤백 불가능한 외부 I/O를 트랜잭션 바깥으로 분리할 때 사용한다.
 * afterCommit 콜백에서 발생한 예외는 트랜잭션이 이미 커밋된 상태이므로 클라이언트에 전파하지 않고 로깅만 한다.
 */
@Component
public class AfterCommitExecutor {

    private static final Logger log = LoggerFactory.getLogger(AfterCommitExecutor.class);

    public void execute(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            try {
                                task.run();
                            } catch (RuntimeException e) {
                                log.warn("afterCommit task execution failed", e);
                            }
                        }
                    });
        } else {
            task.run();
        }
    }
}
