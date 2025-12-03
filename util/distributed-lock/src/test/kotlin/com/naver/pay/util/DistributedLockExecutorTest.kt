package com.naver.pay.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import java.util.concurrent.TimeUnit

class DistributedLockExecutorTest : BehaviorSpec({
    val redissonClient = mockk<RedissonClient>()
    val rLock = mockk<RLock>()
    val distributedLockExecutor = DistributedLockExecutor(redissonClient)

    val lockName = "testLock"
    val key = "testKey"
    val lockKey = "LOCK::$lockName::$key"

    beforeTest {
        every { redissonClient.getLock(lockKey) } returns rLock
    }

    afterTest {
        clearMocks(redissonClient, rLock)
    }

    Given("분산 락 실행기가 주어졌을 때") {

        When("락을 성공적으로 획득하고 로직이 정상적으로 실행되면") {
            every { rLock.tryLock(any(), any(), any()) } returns true
            every { rLock.unlock() } just runs
            every { rLock.isHeldByCurrentThread } returns true

            val result = distributedLockExecutor.execute(lockName, key) {
                "execution success"
            }

            Then("실행 결과를 반환하고 락을 해제한다") {
                result shouldBe "execution success"
                verify(exactly = 1) { redissonClient.getLock(lockKey) }
                verify(exactly = 1) { rLock.tryLock(5, 10, TimeUnit.SECONDS) }
                verify(exactly = 1) { rLock.unlock() }
            }
        }

        When("락 획득에 실패하면") {
            every { rLock.tryLock(any(), any(), any()) } returns false

            Then("RuntimeException이 발생한다") {
                val exception = shouldThrow<RuntimeException> {
                    distributedLockExecutor.execute(lockName, key) {
                        "이 블록은 실행되어서는 안 됩니다"
                    }
                }
                exception.message shouldBe "Lock 획득 실패: $lockName"
                verify(exactly = 0) { rLock.unlock() }
            }
        }

        When("로직 실행 중 예외가 발생하면") {
            every { rLock.tryLock(any(), any(), any()) } returns true
            every { rLock.isHeldByCurrentThread } returns true
            every { rLock.unlock() } just runs
            val runtimeException = RuntimeException("Error in block")

            Then("예외를 전파하고 락은 반드시 해제한다") {
                val thrown = shouldThrow<RuntimeException> {
                    distributedLockExecutor.execute(lockName, key) {
                        throw runtimeException
                    }
                }
                thrown shouldBe runtimeException
                verify(exactly = 1) { rLock.unlock() }
            }
        }

        When("락을 소유하지 않은 스레드가 unlock을 시도하는 경우 (예: leaseTime 초과)") {
            every { rLock.tryLock(any(), any(), any()) } returns true
            every { rLock.isHeldByCurrentThread } returns false
            every { rLock.unlock() } just runs

            distributedLockExecutor.execute(lockName, key) {
                "일부 로직"
            }

            Then("unlock을 호출하지 않는다") {
                verify(exactly = 0) { rLock.unlock() }
            }
        }
    }
})
