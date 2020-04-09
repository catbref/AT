package org.ciyam.at;

import static org.ciyam.at.test.TestUtils.hexToBytes;
import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;

import org.ciyam.at.ExecutionException;
import org.ciyam.at.FunctionCode;
import org.ciyam.at.OpCode;
import org.ciyam.at.test.ExecutableTest;
import org.junit.Test;

public class HashingFunctionCodeTests extends ExecutableTest {

	private static final String message = "The quick, brown fox jumped over the lazy dog.";
	private static final byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);

	private static final FunctionCode[] bSettingFunctions = new FunctionCode[] { FunctionCode.SET_B1, FunctionCode.SET_B2, FunctionCode.SET_B3, FunctionCode.SET_B4 };

	@Test
	public void testMD5() throws ExecutionException {
		testHash("MD5", FunctionCode.MD5_INTO_B, "1388a82384756096e627e3671e2624bf");
	}

	@Test
	public void testCHECK_MD5() throws ExecutionException {
		checkHash("MD5", FunctionCode.CHECK_MD5_WITH_B, "1388a82384756096e627e3671e2624bf");
	}

	@Test
	public void testRMD160() throws ExecutionException {
		testHash("RIPE-MD160", FunctionCode.RMD160_INTO_B, "b5a4b1898af3745dbbb5becb83e72787df9952c9");
	}

	@Test
	public void testCHECK_RMD160() throws ExecutionException {
		checkHash("RIPE-MD160", FunctionCode.CHECK_RMD160_WITH_B, "b5a4b1898af3745dbbb5becb83e72787df9952c9");
	}

	@Test
	public void testSHA256() throws ExecutionException {
		testHash("SHA256", FunctionCode.SHA256_INTO_B, "c01d63749ebe5d6b16f7247015cac2e49a5ac4fb6c7f24bed07b8aa904da97f3");
	}

	@Test
	public void testCHECK_SHA256() throws ExecutionException {
		checkHash("SHA256", FunctionCode.CHECK_SHA256_WITH_B, "c01d63749ebe5d6b16f7247015cac2e49a5ac4fb6c7f24bed07b8aa904da97f3");
	}

	@Test
	public void testHASH160() throws ExecutionException {
		testHash("HASH160", FunctionCode.HASH160_INTO_B, "54d54a03fd447996ab004dee87fab80bf9477e23");
	}

	@Test
	public void testCHECK_HASH160() throws ExecutionException {
		checkHash("HASH160", FunctionCode.CHECK_HASH160_WITH_B, "54d54a03fd447996ab004dee87fab80bf9477e23");
	}

	private void testHash(String hashName, FunctionCode hashFunction, String expected) throws ExecutionException {
		// Data addr 0 for setting values
		dataByteBuffer.putLong(0L);
		// Data addr 1 for results
		dataByteBuffer.putLong(0L);

		// Data addr 2 has start of message bytes (address 4)
		dataByteBuffer.putLong(4L);

		// Data addr 3 has length of message bytes
		dataByteBuffer.putLong(messageBytes.length);

		// Data addr 4+ for message
		dataByteBuffer.put(messageBytes);

		// Actual hash function
		codeByteBuffer.put(OpCode.EXT_FUN_DAT_2.value).putShort(hashFunction.value).putInt(2).putInt(3);

		// Hash functions usually put result into B, but we need it in A
		codeByteBuffer.put(OpCode.EXT_FUN.value).putShort(FunctionCode.SWAP_A_AND_B.value);

		// Expected result goes into B
		loadHashIntoB(expected);

		// Check actual hash output (in A) with expected result (in B) and save equality output into address 1
		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.CHECK_A_EQUALS_B.value).putInt(1);

		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue("MachineState isn't in finished state", state.getIsFinished());
		assertFalse("MachineState encountered fatal error", state.getHadFatalError());
		assertEquals(hashName + " hashes do not match", 1L, getData(1));
	}

	private void checkHash(String hashName, FunctionCode checkFunction, String expected) throws ExecutionException {
		// Data addr 0 for setting values
		dataByteBuffer.putLong(0L);
		// Data addr 1 for results
		dataByteBuffer.putLong(0L);

		// Data addr 2 has start of message bytes (address 4)
		dataByteBuffer.putLong(4L);

		// Data addr 3 has length of message bytes
		dataByteBuffer.putLong(messageBytes.length);

		// Data addr 4+ for message
		dataByteBuffer.put(messageBytes);

		// Expected result goes into B
		loadHashIntoB(expected);

		codeByteBuffer.put(OpCode.EXT_FUN_RET_DAT_2.value).putShort(checkFunction.value).putInt(1).putInt(2).putInt(3);

		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue("MachineState isn't in finished state", state.getIsFinished());
		assertFalse("MachineState encountered fatal error", state.getHadFatalError());
		assertEquals(hashName + " hashes do not match", 1L, getData(1));
	}

	private void loadHashIntoB(String expected) {
		// Expected result goes into B
		codeByteBuffer.put(OpCode.EXT_FUN.value).putShort(FunctionCode.CLEAR_B.value);

		// Each 16 hex-chars (8 bytes) fits into each B word (B1, B2, B3 and B4)
		int numLongs = (expected.length() + 15) / 16;

		for (int longIndex = 0; longIndex < numLongs; ++longIndex) {
			final int endIndex = expected.length() - (numLongs - longIndex - 1) * 16;
			final int beginIndex = Math.max(0, endIndex - 16);

			String hexChars = expected.substring(beginIndex, endIndex);

			codeByteBuffer.put(OpCode.SET_VAL.value);
			codeByteBuffer.putInt(0); // addr 0
			codeByteBuffer.put(new byte[8 - hexChars.length() / 2]); // pad LSB with zeros
			codeByteBuffer.put(hexToBytes(hexChars));

			final FunctionCode bSettingFunction = bSettingFunctions[longIndex];
			codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(bSettingFunction.value).putInt(0);
		}
	}

}
