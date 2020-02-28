import static common.TestUtils.hexToBytes;
import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;

import org.ciyam.at.ExecutionException;
import org.ciyam.at.FunctionCode;
import org.ciyam.at.OpCode;
import org.ciyam.at.Timestamp;
import org.junit.Test;

import common.ExecutableTest;

public class FunctionCodeTests extends ExecutableTest {

	private static final String message = "The quick, brown fox jumped over the lazy dog.";
	private static final byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);

	private static final FunctionCode[] bSettingFunctions = new FunctionCode[] { FunctionCode.SET_B1, FunctionCode.SET_B2, FunctionCode.SET_B3, FunctionCode.SET_B4 };

	@Test
	public void testMD5() throws ExecutionException {
		testHash("MD5", FunctionCode.MD5_A_TO_B, FunctionCode.CHECK_A_EQUALS_B, "1388a82384756096e627e3671e2624bf");
	}

	@Test
	public void testCHECK_MD5() throws ExecutionException {
		testHash("MD5", null, FunctionCode.CHECK_MD5_A_WITH_B, "1388a82384756096e627e3671e2624bf");
	}

	@Test
	public void testRMD160() throws ExecutionException {
		testHash("RIPE-MD160", FunctionCode.RMD160_A_TO_B, FunctionCode.CHECK_A_EQUALS_B, "b5a4b1898af3745dbbb5becb83e72787df9952c9");
	}

	@Test
	public void testCHECK_RMD160() throws ExecutionException {
		testHash("RIPE-MD160", null, FunctionCode.CHECK_RMD160_A_WITH_B, "b5a4b1898af3745dbbb5becb83e72787df9952c9");
	}

	@Test
	public void testSHA256() throws ExecutionException {
		testHash("SHA256", FunctionCode.SHA256_A_TO_B, FunctionCode.CHECK_A_EQUALS_B, "c01d63749ebe5d6b16f7247015cac2e49a5ac4fb6c7f24bed07b8aa904da97f3");
	}

	@Test
	public void testCHECK_SHA256() throws ExecutionException {
		testHash("SHA256", null, FunctionCode.CHECK_SHA256_A_WITH_B, "c01d63749ebe5d6b16f7247015cac2e49a5ac4fb6c7f24bed07b8aa904da97f3");
	}

	@Test
	public void testHASH160() throws ExecutionException {
		testHash("HASH160", FunctionCode.HASH160_A_TO_B, FunctionCode.CHECK_A_EQUALS_B, "54d54a03fd447996ab004dee87fab80bf9477e23");
	}

	@Test
	public void testCHECK_HASH160() throws ExecutionException {
		testHash("HASH160", null, FunctionCode.CHECK_HASH160_A_WITH_B, "54d54a03fd447996ab004dee87fab80bf9477e23");
	}

	@Test
	public void testRandom() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(Timestamp.toLong(api.getCurrentBlockHeight(), 0));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.PUT_TX_AFTER_TIMESTAMP_IN_A.value).putInt(0);
		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.GENERATE_RANDOM_USING_TX_IN_A.value).putInt(1);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(false);

		assertNotEquals("Random wasn't generated", 0L, getData(1));
		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
	}

	@Test
	public void testInvalidFunctionCode() throws ExecutionException {
		codeByteBuffer.put(OpCode.EXT_FUN.value).putShort((short) 0xaaaa);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertTrue(state.getHadFatalError());
	}

	@Test
	public void testPlatformSpecific0501() {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(Timestamp.toLong(api.getCurrentBlockHeight(), 0));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort((short) 0x0501).putInt(0);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
	}

	@Test
	public void testPlatformSpecific0501Error() {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(Timestamp.toLong(api.getCurrentBlockHeight(), 0));
		codeByteBuffer.put(OpCode.EXT_FUN_RET_DAT_2.value).putShort((short) 0x0501).putInt(0).putInt(0); // Wrong OPCODE for function
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertTrue(state.getHadFatalError());
	}

	private void testHash(String hashName, FunctionCode hashFunction, FunctionCode checkFunction, String expected) throws ExecutionException {
		// Data addr 0 for setting values
		dataByteBuffer.putLong(0L);
		// Data addr 1 for results
		dataByteBuffer.putLong(0L);

		// Data addr 2+ for message
		dataByteBuffer.put(messageBytes);

		// MD5 data start
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(2L);
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_A1.value).putInt(0);

		// MD5 data length
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(messageBytes.length);
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_A2.value).putInt(0);

		// A3 unused
		// A4 unused

		// Optional hash function
		if (hashFunction != null) {
			codeByteBuffer.put(OpCode.EXT_FUN.value).putShort(hashFunction.value);
			// Hash functions usually put result into B, but we need it in A
			codeByteBuffer.put(OpCode.EXT_FUN.value).putShort(FunctionCode.SWAP_A_AND_B.value);
		}

		// Expected result goes into B
		codeByteBuffer.put(OpCode.EXT_FUN.value).putShort(FunctionCode.CLEAR_B.value);
		// Each 16 hex-chars (8 bytes) fits into each B word (B1, B2, B3 and B4)
		for (int bWord = 0; bWord < 4 && bWord * 16 < expected.length(); ++bWord) {
			final int beginIndex = bWord * 16;
			final int endIndex = Math.min(expected.length(), beginIndex + 16);

			String hexChars = expected.substring(beginIndex, endIndex);
			codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes(hexChars));
			codeByteBuffer.put(new byte[8 - hexChars.length() / 2]); // pad with zeros

			final FunctionCode bSettingFunction = bSettingFunctions[bWord];
			codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(bSettingFunction.value).putInt(0);
		}

		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(checkFunction.value).putInt(1);

		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue("MachineState isn't in finished state", state.getIsFinished());
		assertFalse("MachineState encountered fatal error", state.getHadFatalError());
		assertEquals(hashName + " hashes do not match", 1L, getData(1));
	}

}
