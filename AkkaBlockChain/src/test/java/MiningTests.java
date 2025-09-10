import akka.actor.testkit.typed.CapturedLogEvent;
import akka.actor.testkit.typed.javadsl.BehaviorTestKit;
import akka.actor.testkit.typed.javadsl.TestInbox;
import blockchain.ManagerBehavior;
import blockchain.WorkerBehavior;
import model.Block;
import model.HashResult;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;
import utils.BlocksData;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class MiningTests {

    @Test
    void testMiningFailsIfNonceNotInRange() {
        BehaviorTestKit<WorkerBehavior.Command> testActor = BehaviorTestKit.create(WorkerBehavior.create());
        Block block = BlocksData.getNextBlock(0, "0");

        TestInbox<ManagerBehavior.Command> testInbox = TestInbox.create();

        WorkerBehavior.Command message = new WorkerBehavior.Command(block, 0, 5, testInbox.getRef());
        testActor.run(message);
        List<CapturedLogEvent> logMessages = testActor.getAllLogEntries();
        assertEquals(1, logMessages.size());
        assertEquals("null", logMessages.get(0).message());
        assertEquals(Level.DEBUG, logMessages.get(0).level());
    }

    @Test
    void testMiningPassesIfNonceIsInRange() {
        BehaviorTestKit<WorkerBehavior.Command> testActor = BehaviorTestKit.create(WorkerBehavior.create());
        Block block = BlocksData.getNextBlock(0, "0");

        TestInbox<ManagerBehavior.Command> testInbox = TestInbox.create();

        WorkerBehavior.Command message = new WorkerBehavior.Command(block, 4385000, 5, testInbox.getRef());
        testActor.run(message);
        List<CapturedLogEvent> logMessages = testActor.getAllLogEntries();
        assertEquals(1, logMessages.size());
        String expectedResult = "4385438 : 000005063c2755396873ec402b09e910c46791dd06acb720cb6ca392ed6e613f";
        assertEquals(expectedResult, logMessages.get(0).message());
        assertEquals(Level.DEBUG, logMessages.get(0).level());
    }

    @Test
    void testMessageReceivedIfNonceInRange() {
        BehaviorTestKit<WorkerBehavior.Command> testActor = BehaviorTestKit.create(WorkerBehavior.create());
        Block block = BlocksData.getNextBlock(0, "0");

        TestInbox<ManagerBehavior.Command> testInbox = TestInbox.create();

        WorkerBehavior.Command message = new WorkerBehavior.Command(block, 4385000, 5, testInbox.getRef());
        testActor.run(message);

        HashResult expectedHashResult = new HashResult();
        expectedHashResult.foundAHash("000005063c2755396873ec402b09e910c46791dd06acb720cb6ca392ed6e613f", 4385438);
        ManagerBehavior.Command expectedCommand = new ManagerBehavior.HashResultCommand(expectedHashResult);
        testInbox.expectMessage(expectedCommand);
    }

    @Test
    void testNoMessageReceivedIfNonceNotInRange() {
        BehaviorTestKit<WorkerBehavior.Command> testActor = BehaviorTestKit.create(WorkerBehavior.create());
        Block block = BlocksData.getNextBlock(0, "0");

        TestInbox<ManagerBehavior.Command> testInbox = TestInbox.create();

        WorkerBehavior.Command message = new WorkerBehavior.Command(block, 0, 5, testInbox.getRef());
        testActor.run(message);

        assertFalse(testInbox.hasMessages());
    }
}
