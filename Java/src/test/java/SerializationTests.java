import static common.TestUtils.hexToBytes;
import static org.junit.Assert.*;

import java.util.Arrays;

import org.ciyam.at.ExecutionException;
import org.ciyam.at.FunctionCode;
import org.ciyam.at.MachineState;
import org.ciyam.at.OpCode;
import org.junit.Test;

import common.ExecutableTest;
import common.TestUtils;

public class SerializationTests extends ExecutableTest {

	private byte[] simulate() {
		byte[] headerBytes = TestUtils.HEADER_BYTES;
		byte[] codeBytes = codeByteBuffer.array();
		byte[] dataBytes = new byte[0];

		state = new MachineState(api, logger, headerBytes, codeBytes, dataBytes);

		return executeAndCheck(state);
	}

	private byte[] continueSimulation(byte[] savedState) {
		byte[] codeBytes = codeByteBuffer.array();
		state = MachineState.fromBytes(api, logger, savedState, codeBytes);

		// Pretend we're on next block
		api.bumpCurrentBlockHeight();

		return executeAndCheck(state);
	}

	private byte[] executeAndCheck(MachineState state) {
		state.execute();

		// Fetch current state, and code bytes
		byte[] stateBytes = unwrapState(state);
		byte[] codeBytes = state.getCodeBytes();

		// Rebuild new MachineState using fetched state & bytes
		MachineState restoredState = MachineState.fromBytes(api, logger, stateBytes, codeBytes);
		// Extract rebuilt state and code bytes
		byte[] restoredStateBytes = restoredState.toBytes();
		byte[] restoredCodeBytes = state.getCodeBytes();

		// Check that both states and bytes match
		assertTrue("Serialization->Deserialization->Reserialization error", Arrays.equals(stateBytes, restoredStateBytes));
		assertTrue("Serialization->Deserialization->Reserialization error", Arrays.equals(codeBytes, restoredCodeBytes));

		return stateBytes;
	}

	/** Test serialization of state with stop address. */
	@Test
	public void testPCS2() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("0000000011111111"));

		codeByteBuffer.put(OpCode.SET_PCS.value);
		int expectedStopAddress = codeByteBuffer.position();

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("0000000022222222"));

		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertEquals(expectedStopAddress, (int) state.getOnStopAddress());
		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
	}

	/** Test serialization of state with data pushed onto user stack. */
	@Test
	public void testStopWithStacks() throws ExecutionException {
		long initialValue = 100L;
		long increment = 10L;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(initialValue); // 0000

		codeByteBuffer.put(OpCode.SET_PCS.value); // 000d
		int expectedStopAddress = codeByteBuffer.position();

		codeByteBuffer.put(OpCode.JMP_SUB.value).putInt(0x002a); // 000e

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(increment); // 0013

		codeByteBuffer.put(OpCode.ADD_DAT.value).putInt(0).putInt(1); // 0020

		codeByteBuffer.put(OpCode.STP_IMD.value); // 0029

		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.ECHO.value).putInt(0); // 002a

		codeByteBuffer.put(OpCode.PSH_DAT.value).putInt(0); // 0031

		codeByteBuffer.put(OpCode.RET_SUB.value); // 0036

		byte[] savedState = simulate();

		assertEquals(expectedStopAddress, (int) state.getOnStopAddress());
		assertTrue(state.getIsStopped());
		assertFalse(state.getHadFatalError());

		// Just after first STP_IMD we expect address 0 to be initialValue + increment
		long expectedValue = initialValue + increment;
		assertEquals(expectedValue, getData(0));

		// Perform another execution round
		savedState = continueSimulation(savedState);
		expectedValue += increment;
		assertEquals(expectedValue, getData(0));

		savedState = continueSimulation(savedState);
		expectedValue += increment;
		assertEquals(expectedValue, getData(0));
	}

}
