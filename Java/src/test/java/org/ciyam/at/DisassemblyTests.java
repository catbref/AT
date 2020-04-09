package org.ciyam.at;

import static org.ciyam.at.test.TestUtils.hexToBytes;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.util.Random;

import org.ciyam.at.ExecutionException;
import org.ciyam.at.FunctionCode;
import org.ciyam.at.MachineState;
import org.ciyam.at.OpCode;
import org.ciyam.at.test.ExecutableTest;
import org.ciyam.at.test.TestUtils;
import org.junit.Test;

public class DisassemblyTests extends ExecutableTest {

	private static final String message = "The quick, brown fox jumped over the lazy dog.";
	private static final byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);

	@Test
	public void testRMD160disassembly() throws ExecutionException {
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

		// RMD160 of "The quick, brown fox jumped over the lazy dog." is b5a4b1898af3745dbbb5becb83e72787df9952c9
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("00000000b5a4b189"));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_B1.value).putInt(0);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("8af3745dbbb5becb"));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_B2.value).putInt(0);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("83e72787df9952c9"));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_B3.value).putInt(0);

		codeByteBuffer.put(OpCode.EXT_FUN_RET_DAT_2.value).putShort(FunctionCode.CHECK_RMD160_WITH_B.value).putInt(1).putInt(2).putInt(3);

		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.ECHO.value).putInt(1);

		codeByteBuffer.put(OpCode.FIN_IMD.value);

		byte[] codeBytes = TestUtils.getBytes(codeByteBuffer);
		int dataBufferLength = dataByteBuffer.position();

		System.out.println(MachineState.disassemble(codeBytes, dataBufferLength));
	}

	@Test
	public void testFuzzyDisassembly() {
		Random random = new Random();

		byte[] randomCode = new byte[200];
		random.nextBytes(randomCode);

		int dataBufferLength = 1024;

		try {
			System.out.println(MachineState.disassemble(randomCode, dataBufferLength));
		} catch (ExecutionException e) {
			// we expect this to fail
			return;
		}

		fail("Random code should cause disassembly failure");
	}

}
