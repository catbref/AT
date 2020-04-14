package org.ciyam.at;

import static org.junit.Assert.*;

import org.ciyam.at.ExecutionException;
import org.ciyam.at.FunctionCode;
import org.ciyam.at.MachineState;
import org.ciyam.at.OpCode;
import org.ciyam.at.test.ExecutableTest;
import org.ciyam.at.test.TestAPI;
import org.ciyam.at.test.TestUtils;
import org.junit.Test;

public class MiscTests extends ExecutableTest {

	@Test
	public void testSimpleCode() throws ExecutionException {
		long testValue = 8888L;
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(testValue);
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.ECHO.value).putInt(0);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.isFinished());
		assertFalse(state.hadFatalError());
		assertEquals("Data does not match", testValue, getData(0));
	}

	@Test
	public void testInvalidOpCode() throws ExecutionException {
		codeByteBuffer.put((byte) 0xdd);

		execute(true);

		assertTrue(state.isFinished());
		assertTrue(state.hadFatalError());
	}

	@Test
	public void testFreeze() throws ExecutionException {
		// Infinite loop
		codeByteBuffer.put(OpCode.JMP_ADR.value).putInt(0);

		// We need enough rounds to exhaust balance
		long minRounds = TestAPI.DEFAULT_INITIAL_BALANCE / TestAPI.MAX_STEPS_PER_ROUND + 1;
		for (long i = 0; i < minRounds; ++i)
			execute(true);

		assertTrue(state.isFrozen());

		Long frozenBalance = state.getFrozenBalance();
		assertNotNull(frozenBalance);
	}

	@Test
	public void testMinActivation() throws ExecutionException {
		// Make sure minimum activation amount is greater than initial balance
		long minActivationAmount = TestAPI.DEFAULT_INITIAL_BALANCE * 2L;

		byte[] headerBytes = TestUtils.toHeaderBytes(TestUtils.VERSION, TestUtils.NUM_CODE_PAGES, TestUtils.NUM_DATA_PAGES, TestUtils.NUM_CALL_STACK_PAGES, TestUtils.NUM_USER_STACK_PAGES, minActivationAmount);
		byte[] codeBytes = codeByteBuffer.array();
		byte[] dataBytes = new byte[0];

		state = new MachineState(api, loggerFactory, headerBytes, codeBytes, dataBytes);

		assertTrue(state.isFrozen());
		assertEquals((Long) (minActivationAmount - 1L), state.getFrozenBalance());
	}

}
