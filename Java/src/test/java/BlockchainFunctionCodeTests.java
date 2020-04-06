import static org.junit.Assert.*;

import java.util.Arrays;

import org.ciyam.at.ExecutionException;
import org.ciyam.at.FunctionCode;
import org.ciyam.at.OpCode;
import org.ciyam.at.Timestamp;
import org.junit.Test;

import common.ExecutableTest;
import common.TestAPI;
import common.TestAPI.TestBlock;
import common.TestAPI.TestTransaction;

public class BlockchainFunctionCodeTests extends ExecutableTest {

	/**
	 * GET_BLOCK_TIMESTAMP
	 * GET_CREATION_TIMESTAMP
	 * GET_PREVIOUS_BLOCK_TIMESTAMP
	 * PUT_PREVIOUS_BLOCK_HASH_INTO_A
	 * PUT_TX_AFTER_TIMESTAMP_INTO_A
	 * GET_TYPE_FROM_TX_IN_A
	 * GET_AMOUNT_FROM_TX_IN_A
	 * GET_TIMESTAMP_FROM_TX_IN_A
	 * GENERATE_RANDOM_USING_TX_IN_A
	 * PUT_MESSAGE_FROM_TX_IN_A_INTO_B
	 * PUT_ADDRESS_FROM_TX_IN_A_INTO_B
	 * PUT_CREATOR_INTO_B
	 * GET_CURRENT_BALANCE
	 * GET_PREVIOUS_BALANCE
	 * PAY_TO_ADDRESS_IN_B
	 * PAY_ALL_TO_ADDRESS_IN_B
	 * PAY_PREVIOUS_TO_ADDRESS_IN_B
	 * MESSAGE_A_TO_ADDRESS_IN_B
	 * ADD_MINUTES_TO_TIMESTAMP
	 */

	@Test
	public void testGetBlockTimestamp() throws ExecutionException {
		// Grab block 'timestamp' and save into address 0
		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.GET_BLOCK_TIMESTAMP.value).putInt(0);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		Timestamp blockTimestamp = new Timestamp(getData(0));
		assertEquals("Block timestamp incorrect", TestAPI.DEFAULT_INITIAL_BLOCK_HEIGHT, blockTimestamp.blockHeight);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
	}

	@Test
	public void testMultipleGetBlockTimestamp() throws ExecutionException {
		int expectedBlockHeight = TestAPI.DEFAULT_INITIAL_BLOCK_HEIGHT;

		// Grab block 'timestamp' and save into address 0
		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.GET_BLOCK_TIMESTAMP.value).putInt(0);
		codeByteBuffer.put(OpCode.STP_IMD.value);

		execute(true); // TestAPI's block height bumped prior to return

		Timestamp blockTimestamp = new Timestamp(getData(0));
		assertEquals("Block timestamp incorrect", expectedBlockHeight, blockTimestamp.blockHeight);

		// Re-test
		++expectedBlockHeight;
		execute(true); // TestAPI's block height bumped prior to return

		blockTimestamp = new Timestamp(getData(0));
		assertEquals("Block timestamp incorrect", expectedBlockHeight, blockTimestamp.blockHeight);

		assertFalse(state.getHadFatalError());
	}

	@Test
	public void testGetCreationTimestamp() throws ExecutionException {
		// Grab AT creation 'timestamp' and save into address 0
		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.GET_CREATION_TIMESTAMP.value).putInt(0);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		Timestamp blockTimestamp = new Timestamp(getData(0));
		assertEquals("Block timestamp incorrect", TestAPI.DEFAULT_AT_CREATION_BLOCK_HEIGHT, blockTimestamp.blockHeight);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
	}

	@Test
	public void testGetPreviousBlockTimestamp() throws ExecutionException {
		int expectedBlockHeight = TestAPI.DEFAULT_INITIAL_BLOCK_HEIGHT - 1;

		// Grab previous block 'timestamp' and save into address 0
		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.GET_PREVIOUS_BLOCK_TIMESTAMP.value).putInt(0);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		Timestamp blockTimestamp = new Timestamp(getData(0));
		assertEquals("Block timestamp incorrect", expectedBlockHeight, blockTimestamp.blockHeight);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
	}

	@Test
	public void testPutPreviousBlockHashIntoA() throws ExecutionException {
		int previousBlockHeight = TestAPI.DEFAULT_INITIAL_BLOCK_HEIGHT - 1;

		codeByteBuffer.put(OpCode.EXT_FUN.value).putShort(FunctionCode.PUT_PREVIOUS_BLOCK_HASH_INTO_A.value);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		byte[] expectedBlockHash = api.blockchain.get(previousBlockHeight - 1).blockHash;

		byte[] aBytes = state.getA();
		assertTrue("Block hash mismatch", Arrays.equals(expectedBlockHash, aBytes));

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
	}

	@Test
	public void testPutTransactionAfterTimestampIntoA() throws ExecutionException {
		long initialTimestamp = Timestamp.toLong(TestAPI.DEFAULT_INITIAL_BLOCK_HEIGHT, 0);
		dataByteBuffer.putLong(initialTimestamp);

		// Generate some blocks containing transactions (but none to AT)
		api.generateBlockWithNonAtTransactions();
		api.generateBlockWithNonAtTransactions();
		// Generate a block containing transaction to AT
		api.generateBlockWithAtTransaction();

		int currentBlockHeight = api.blockchain.size();
		api.setCurrentBlockHeight(currentBlockHeight);

		// Fetch transaction signature/hash after timestamp stored in address 0
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.PUT_TX_AFTER_TIMESTAMP_INTO_A.value).putInt(0);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		TestTransaction transaction = api.getTransactionFromA(state);
		assertNotNull(transaction);

		Timestamp txTimestamp = new Timestamp(transaction.timestamp);
		assertEquals("Transaction hash mismatch", currentBlockHeight, txTimestamp.blockHeight);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
	}

	@Test
	public void testPutNoTransactionAfterTimestampIntoA() throws ExecutionException {
		int initialBlockHeight = TestAPI.DEFAULT_INITIAL_BLOCK_HEIGHT;
		long initialTimestamp = Timestamp.toLong(initialBlockHeight, 0);
		dataByteBuffer.putLong(initialTimestamp);

		// Generate a block containing transaction to AT
		api.generateBlockWithAtTransaction();
		api.bumpCurrentBlockHeight();
		api.generateBlockWithAtTransaction();
		api.bumpCurrentBlockHeight();

		long expectedTransactionsCount = 0;
		for (int blockHeight = initialBlockHeight + 1; blockHeight <= api.blockchain.size(); ++blockHeight) {
			TestBlock block = api.blockchain.get(blockHeight - 1);
			expectedTransactionsCount += block.transactions.stream().filter(transaction -> transaction.recipient.equals(TestAPI.AT_ADDRESS)).count();
		}

		// Count how many transactions after timestamp
		int targetPosition = 0x15;

		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.PUT_TX_AFTER_TIMESTAMP_INTO_A.value).putInt(0);
		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.CHECK_A_IS_ZERO.value).putInt(1);
		int bzrPosition = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BZR_DAT.value).putInt(1).put((byte) (targetPosition - bzrPosition));
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		assertEquals("targetPosition incorrect", targetPosition, codeByteBuffer.position());
		// Update latest timestamp in address 0
		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.GET_TIMESTAMP_FROM_TX_IN_A.value).putInt(0);
		// Increment transactions count
		codeByteBuffer.put(OpCode.INC_DAT.value).putInt(2);
		// Loop again
		codeByteBuffer.put(OpCode.JMP_ADR.value).putInt(0);

		execute(true);

		long transactionsCount = getData(2);
		assertEquals("Transaction count incorrect", expectedTransactionsCount, transactionsCount);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
	}

	@Test
	public void testRandom() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(Timestamp.toLong(api.getCurrentBlockHeight(), 0));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.PUT_TX_AFTER_TIMESTAMP_INTO_A.value).putInt(0);
		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.GENERATE_RANDOM_USING_TX_IN_A.value).putInt(1);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// Generate a block containing transaction to AT
		api.generateBlockWithAtTransaction();

		execute(false);

		assertNotEquals("Random wasn't generated", 0L, getData(1));
		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
	}

}
