package com.markaldrich.chip8;

import com.markaldrich.jgl.JGLGame;
import com.markaldrich.jgl.JGLGameController;
import com.markaldrich.jgl.JGLGameProperties;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.Random;

/**
 * Created by maste on 3/24/2016.
 */
public class Main extends JGLGame {
	
	public static boolean DEBUGGING = true;
	
	private JGLGameProperties props;
	private static File file;
	
	public static int[] chip8_fontset = {
		0xF0, 0x90, 0x90, 0x90, 0xF0, // 0
		0x20, 0x60, 0x20, 0x20, 0x70, // 1
		0xF0, 0x10, 0xF0, 0x80, 0xF0, // 2
		0xF0, 0x10, 0xF0, 0x10, 0xF0, // 3
		0x90, 0x90, 0xF0, 0x10, 0x10, // 4
		0xF0, 0x80, 0xF0, 0x10, 0xF0, // 5
		0xF0, 0x80, 0xF0, 0x90, 0xF0, // 6
		0xF0, 0x10, 0x20, 0x40, 0x40, // 7
		0xF0, 0x90, 0xF0, 0x90, 0xF0, // 8
		0xF0, 0x90, 0xF0, 0x10, 0xF0, // 9
		0xF0, 0x90, 0xF0, 0x90, 0x90, // A
		0xE0, 0x90, 0xE0, 0x90, 0xE0, // B
		0xF0, 0x80, 0x80, 0x80, 0xF0, // C
		0xE0, 0x90, 0x90, 0x90, 0xE0, // D
		0xF0, 0x80, 0xF0, 0x80, 0xF0, // E
		0xF0, 0x80, 0xF0, 0x80, 0x80  // F
	};
	
	/*
	 * Emulator variables
	 */
	// Even though the opcode is a short, it is unsigned, so a Java signed short cannot
	// be used
	public int opcode;
	
	// Main memory of the Chip8
	// Mapped out as:
	/**
	 * 0x000-0x1FF - Chip 8 interpreter (contains font set in emu)
	 * 0x050-0x0A0 - Used for the built in 4x5 pixel font set (0-F)
	 * 0x200-0xFFF - Program ROM and work RAM
	 */
	public int[] memory;
	
	// CPU registers
	public int[] V;
	
	// Index register
	public int I;
	
	// Program counter
	public int pc;
	
	// Graphics memory
	public int[] gfx;
	
	// Two countdown registers
	public int delayTimer;
	// TODO: The emulator will make a sound when this register gets to 0.
	public int soundTimer;
	
	// Stack and stack pointer
	public int[] stack;
	public int sp;
	
	// Keyboard memory
	public int[] key;
	
	// Draw flag
	public boolean drawFlag;
	
	public Main(JGLGameProperties props) {
		super(props);
		this.props = props;
	}
	
	@Override
	public void initGame() {
		// TODO: Set up graphics and input
		reset();
		
		// TODO: Load fontset
		
		byte[] data = null;
		try {
			data = Files.readAllBytes(file.toPath());
		} catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		for(int i = 0; i < data.length; i++) {
			write((int) data[i], i + 0x200);
		}
	}
	
	public void write(int value, int location) {
		if(location > 0x1000 || location < 0) {
			System.out.println("Invalid write! Program tried to write 0x" + Integer.toHexString(value)
				 + " to location 0x" + Integer.toHexString(location));
			System.exit(1);
		} else {
			memory[location] = value & 0xFF;
		}
	}
	
	public int read(int location) {
		if(location > 0x1000 || location < 0) {
			System.out.println("Invalid read! Program tried to read from " + Integer.toHexString(location));
			System.exit(1);
			return 0;
		} else {
			return memory[location] & 0xFF;
		}
	}
	
	@Override
	public void render(Graphics2D g) {
		g.setColor(Color.WHITE);
		g.setFont(new Font("Courier New", Font.PLAIN, 12));
		g.drawString("The name of the rom is " + file.getName(), 0, 12);
		
		if(checkDrawFlag()) {
			BufferedImage image = new BufferedImage(64, 32, BufferedImage.TYPE_INT_RGB);
			Graphics2D graphics2D = (Graphics2D) image.getGraphics();
			for(int y = 0; y < 32; y++) {
				for(int x = 0; x < 64; x++) {
					if(gfx[x * y] != 0) {
						// TODO: draw pixel, not 1x1 rectangle
						graphics2D.fillRect(x, y, 1, 1);
					}
				}
			}
			
			g.drawImage(image, 0, 0, props.getGameWidth(), props.getGameHeight(), null);
		}
	}
	
	public void reset() {
		pc = 0x200;
		opcode = 0;
		I = 0;
		sp = 0;
		delayTimer = 0;
		soundTimer = 0;
		memory = new int[4096];
		V = new int[16];
		gfx = new int[64 * 32];
		stack = new int[16];
		key = new int[16];
		drawFlag = false;
		for(int i = 0; i < 80; i++) {
			memory[i] = chip8_fontset[i];
		}
	}
	
	@Override
	public void update() {
		if(!DEBUGGING) {
			// Fetch opcode
			opcode = memory[pc] << 8 | memory[pc + 1];
			
			// TODO: Use delta timing to actually run the CPU at the correct frequency
			// it is currently running at 60hz
			cycle();
			
			// Update keyboard
			if(checkIfKeyIsDown(KeyEvent.VK_X)) {
				key[0] = 1;
			} else {
				key[0] = 0;
			}
			if(checkIfKeyIsDown(KeyEvent.VK_1)) {
				key[1] = 1;
			} else {
				key[1] = 0;
			}
			if(checkIfKeyIsDown(KeyEvent.VK_2)) {
				key[2] = 1;
			} else {
				key[2] = 0;
			}
			if(checkIfKeyIsDown(KeyEvent.VK_3)) {
				key[3] = 1;
			} else {
				key[3] = 0;
			}
			if(checkIfKeyIsDown(KeyEvent.VK_Q)) {
				key[4] = 1;
			} else {
				key[4] = 0;
			}
			if(checkIfKeyIsDown(KeyEvent.VK_W)) {
				key[5] = 1;
			} else {
				key[5] = 0;
			}
			if(checkIfKeyIsDown(KeyEvent.VK_E)) {
				key[6] = 1;
			} else {
				key[6] = 0;
			}
			if(checkIfKeyIsDown(KeyEvent.VK_A)) {
				key[7] = 1;
			} else {
				key[7] = 0;
			}
			if(checkIfKeyIsDown(KeyEvent.VK_S)) {
				key[8] = 1;
			} else {
				key[8] = 0;
			}
			if(checkIfKeyIsDown(KeyEvent.VK_D)) {
				key[9] = 1;
			} else {
				key[9] = 0;
			}
			if(checkIfKeyIsDown(KeyEvent.VK_Z)) {
				key[0xA] = 1;
			} else {
				key[0xA] = 0;
			}
			if(checkIfKeyIsDown(KeyEvent.VK_C)) {
				key[0xB] = 1;
			} else {
				key[0xB] = 0;
			}
			if(checkIfKeyIsDown(KeyEvent.VK_4)) {
				key[0xC] = 1;
			} else {
				key[0xC] = 0;
			}
			if(checkIfKeyIsDown(KeyEvent.VK_R)) {
				key[0xD] = 1;
			} else {
				key[0xD] = 0;
			}
			if(checkIfKeyIsDown(KeyEvent.VK_F)) {
				key[0xE] = 1;
			} else {
				key[0xE] = 0;
			}
			if(checkIfKeyIsDown(KeyEvent.VK_V)) {
				key[0xF] = 1;
			} else {
				key[0xF] = 0;
			}
		} else {
			if(!test00E0()) {
				System.out.println("0x00E0 failed");
			}
			if(!test00EE()) {
				System.out.println("0x00EE failed pc=" + Integer.toHexString(pc));
				for(int i = 0; i < stack.length; i++) {
					System.out.println("" + i + ": 0x" + Integer.toHexString(stack[i]));
				}
			}
			if(!test1NNN()) {
				System.out.println("0x1NNN failed");
			}
			if(!test2NNN()) {
				System.out.println("0x2NNN failed");
			}
			if(!test3XNN_1()) {
				System.out.println("0x3XNN_1 failed");
			}
			if(!test3XNN_2()) {
				System.out.println("0x3XNN_2 failed");
			}
			if(!test4XNN_1()) {
				System.out.println("0x4XNN_1 failed");
			}
			if(!test4XNN_2()) {
				System.out.println("0x4XNN_2 failed");
			}
			if(!test5XY0_1()) {
				System.out.println("0x5XY0_1 failed");
			}
			if(!test5XY0_2()) {
				System.out.println("0x5XY0_2 failed");
			}
			if(!test6XNN()) {
				System.out.println("0x6XNN failed");
			}
			if(!test7XNN()) {
				System.out.println("0x7XNN failed");
			}
			
			if(!testDXYN()) {
				System.out.println("0xDXYN failed");
			}
		}
	}
	
	public void cycle() {
		System.out.println("opcode -> 0x" + Integer.toHexString(opcode));
		
		// Decode and execute opcode
		switch(opcode & 0xF000) {
			case 0x0000: {
				if(opcode == 0x00E0) {
					// 0x00E0
					// Clear the screen
					for(int i = 0; i < gfx.length; i++) {
						gfx[i] = 0;
					}
				} else if(opcode == 0x00EE) {
					// 0x00EE
					// TODO: just a guess
					pc = popFromStack() - 2;
				}
				break;
			}
			case 0x1000: {
				// TODO: good?
				pc = (opcode & 0x0FFF) - 2;
				break;
			}
			case 0x2000: {
				System.out.println("0x2NNN executed");
				pushToStack(pc);
				pc = (opcode & 0x0FFF) - 2;
				break;
			}
			case 0x3000: {
				int registerIndex = (opcode & 0x0F00) >> 8;
				int indexValue = V[registerIndex];
				if(indexValue == (opcode & 0x00FF)) {
					// Skip an instruction here, another one will be skipped when this method
					// is over.
					pc += 2;
				}
				break;
			}
			case 0x4000: {
				int registerIndex = (opcode & 0x0F00) >> 8;
				int indexValue = V[registerIndex];
				if((indexValue & 0x00FF) != (opcode & 0x00FF)) {
					// Skip an instruction
					pc += 2;
				}
				break;
			}
			case 0x5000: {
				int x = (opcode & 0x0F00) >> 8;
				int y = (opcode & 0x00F0) >> 4;
				if(V[x] == V[y]) {
					// Skip an instruction
					pc += 2;
				}
 				break;
			}
			case 0x6000: {
				int x = (opcode & 0x0F00) >> 8;
				int nn = opcode & 0x00FF;
				V[x] = nn;
				break;
			}
			case 0x7000: {
				int x = (opcode & 0x0F00) >> 8;
				int nn = opcode & 0x00FF;
				int temp = V[x] + (opcode & 0x00FF);
				V[x] = (temp < 256) ? temp : (temp - 256);
				break;
			}
			case 0x8000: {
				int x = (opcode & 0x0F00) >> 8;
				int y = (opcode & 0x00F0) >> 4;
				switch(opcode & 0x000F) {
					case 0x0000: {
						V[x] = V[y];
						break;
					}
					case 0x0001: {
						V[x] |= V[y];
						break;
					}
					case 0x0002: {
						V[x] &= V[y];
						break;
					}
					case 0x0003: {
						V[x] ^= V[y];
						break;
					}
					case 0x0004: {
						V[x] += V[y];
						if(V[x] > 0xFF) {
							// If the resulting value is over 0xFF, or the maximum, set the carry flag
							V[0xF] = 1;
							V[x] -= 256;
						}
						break;
					}
					case 0x0005: {
						if(V[x] > V[y]) {
							V[0xF] = 0;
							V[x] -= V[y];
						} else {
							V[0xF] = 1;
							V[x] -= V[y] + 256;
						}
						break;
					}
					case 0x0006: {
						V[0xF] = V[x] & 0x1;
						V[x] = V[x] >> 1;
						// TODO: Maybe? V[x] &= 0xFF;
						break;
					}
					case 0x0007: {
						if(V[y] > V[x]) {
							V[0xF] = 1;
							V[y] -= V[x];
						} else {
							V[0xF] = 1;
							V[y] -= V[x] + 256;
						}
						break;
					}
					case 0x000E: {
						V[0xF] = ((V[x] & 0x80) >> 8);
						V[x] = V[x] << 1;
						break;
					}
				}
				break;
			}
			case 0x9000: {
				int x = (opcode & 0x0F00) >> 8;
				int y = (opcode & 0x00F0) >> 4;
				if(V[x] != V[y]) {
					pc += 2;
				}
				break;
			}
			case 0xA000: {
				I = opcode & 0x0FFF;
				break;
			}
			case 0xB000: {
				int nnn = opcode & 0x0FFF;
				pc = nnn + (V[0] & 0xFF) - 2;
				// pc = nnn + (I & 0xFF);
				break;
			}
			case 0xC000: {
				int x = (opcode & 0x0F00) >> 8;
				int nn = (opcode & 0x00FF);
				V[x] = nn & (new Random().nextInt(256));
				break;
			}
			case 0xD000: {
				/*
				int x = (opcode & 0x0F00) >> 8;
				int y = (opcode & 0x00F0) >> 4;
				int height = (opcode & 0x000F);
				int pixel;
				
				V[0xF] = 0;
				for(int yLine = 0; yLine < height; yLine++) {
					pixel = read(I + yLine);
					for(int xLine = 0; xLine < 8; xLine++) {
						if((pixel & (0x80 >> xLine)) != 0) {
							if(gfx[(x + xLine + ((y + yLine) * 64))] == 1) {
								V[0xF] = 1;
							} else {
								V[0xF] = 0;
							}
							gfx[x + xLine + ((y + yLine) * 64)] ^= 1;
						}
					}
				}
				
				drawFlag = true;
				break;
				*/
				/*
				int xRegister = (opcode & 0x0F00) >> 8;
				int yRegister = (opcode & 0x00F0) >> 4;
				int xPos = V[xRegister];
				int yPos = V[yRegister];
				V[0xF] = 0;
				
				for (int yIndex = 0; yIndex < (opcode & 0xF); yIndex++) {
					
					int colorByte = read(I + yIndex);
					int yCoord = yPos + yIndex;
					yCoord = yCoord % 32;
					
					int mask = 0x80;
					
					for (int xIndex = 0; xIndex < 8; xIndex++) {
						int xCoord = xPos + xIndex;
						xCoord = xCoord % 64;
						
						boolean turnOn = (colorByte & mask) > 0;
						boolean currentOn = gfx[yCoord * 64 + xCoord] != 0;
						
						if (turnOn && currentOn) {
							V[0xF] |= 1;
							turnOn = false;
						} else if (!turnOn && currentOn) {
							turnOn = true;
						}
						
						gfx[yCoord * 64 + xCoord] = (turnOn) ? 1 : 0;
						// mScreen.drawPixel(xCoord, yCoord, turnOn);
						mask = mask >> 1;
					}
				}
				drawFlag = true;
				break;
				*/
				/*
				int x = V[(opcode & 0x0F00) >> 8];
				int y = V[(opcode & 0x00F0) >> 4];
				int height = opcode & 0x000F;
				int pixel;
				
				V[0xF] = 0;
				for (int yline = 0; yline < height; yline++)
				{
					pixel = memory[I + yline];
					for(int xline = 0; xline < 8; xline++)
					{
						if((pixel & (0x80 >> xline)) != 0)
						{
							if(gfx[(x + xline + ((y + yline) * 64))] == 1)
								V[0xF] = 1;
							gfx[x + xline + ((y + yline) * 64)] ^= 1;
						}
					}
				}
				
				drawFlag = true;
				break;
				*/
				
				
				int rows;
				int x;
				int y;
				
				rows = opcode & 0x000F;
				
				int reg = opcode & 0x0F00;
				reg >>= 8;
				
				int reg2 = opcode & 0x00F0;
				reg2 >>= 4;
				
				x = V[reg];
				y = V[reg2];
				
				V[0xF] = 0;
				for (int row = 0; row < rows; row++) {
					int bits = memory[I + row];
					for (int column = 0; column < 8; column++) {
						int index = (y + row) * 64 // Row offset
								+ x // X coordinate
								+ column;
						
						if ((bits & (0x80 >> column)) > 0) {
							if (gfx[index] != 0) {
								V[0xF] = 1;
							}
							gfx[index] ^= 1;
						}
					}
				}
				
				drawFlag = true;
				break;
				
				/*
				int X = (opcode & 0x0F00) >> 8;
				int Y = (opcode & 0x00F0) >> 4;
				int N = (opcode & 0x000F);
				if (N == 0) N = 16;
				for (int yline = 0; yline < N; yline++)
				{
					int data = memory[I + yline];
					for (int xpix = 0; xpix < 8; xpix++)
					{
						if((data & (0x80 >> xpix)) != 0)
						{
							if ((V[X] + xpix) < 64 && (V[Y] + yline) < 32 && (V[X] + xpix) >= 0 && (V[Y] + yline) >= 0)
							{
								if (gfx[(V[X] + xpix)*2][(V[Y] + yline)*2] == 1) V[0xF] = 1;
								gfx[(V[X] + xpix)*2][(V[Y] + yline)*2] ^= 1;
								gfx[(V[X] + xpix)*2 + 1][(V[Y] + yline)*2] ^= 1;
								gfx[(V[X] + xpix)*2][(V[Y] + yline)*2 + 1] ^= 1;
								gfx[(V[X] + xpix)*2 + 1][(V[Y] + yline)*2 + 1] ^= 1;
							}
						}
					}
				}
				drawFlag = true;
				break;
				*/
			}
			case 0xE000: {
				int x = (opcode & 0x0F00) >> 8;
				// TODO: There is probably a much simpler way to do this, but who knows...
				if((opcode & 0x00FF) == 0x009E) {
					if(key[x] != 0) {
						pc += 2;
					}
				} else {
					if(key[x] == 0) {
						pc += 2;
					}
				}
				break;
			}
			case 0xF000: {
				int x = (opcode & 0x0F00) >> 8;
				switch(opcode & 0x00FF) {
					case 0x07: {
						V[x] = delayTimer;
						break;
					}
					case 0x0A: {
						int keyPressed = -1;
						loop: while(true) {
							for(int i = 0; i < key.length; i++) {
								if(key[i] != 0) {
									keyPressed = i;
									break loop;
								}
							}
						}
						V[x] = keyPressed;
						break;
					}
					case 0x15: {
						delayTimer = V[x];
						break;
					}
					case 0x18: {
						soundTimer = V[x];
						break;
					}
					case 0x1E: {
						I += V[x];
						break;
					}
					case 0x29: {
						I = V[x] * 5;
						break;
					}
					case 0x33: {
						int value = V[x];
						write(value / 100, I);
						write((value % 100) / 10, I + 1);
						write((value % 100) % 10, I + 2);
						break;
					}
					case 0x55: {
						for(int i = 0; i <= x; i++) {
							write(V[i], I + i);
						}
						break;
					}
					case 0x65: {
						for(int i = 0; i <= x; i++) {
							V[i] = read(I + i);
						}
						break;
					}
				}
			}
			default: {
				System.out.println("Unknown opcode: 0x" + Integer.toHexString(opcode));
			}
		}
		
		pc += 2;
		
		
		// Update timers (wow so they actually go down at 60hz which is perfect)
		if(delayTimer > 0) {
			delayTimer--;
		}
		if(soundTimer > 0) {
			if(soundTimer == 1) {
				// TODO: beep
				System.out.println("beep");
			}
			soundTimer--;
		}
	}
	
	public boolean checkDrawFlag() {
		return drawFlag;
	}
	
	public void pushToStack(int x) {
		stack[sp++] = x;
	}
	
	public int popFromStack() {
		return stack[--sp];
	}
	
	public static void main(String[] args) {
		if(!DEBUGGING) {
			while(true) {
				JFileChooser chooser = new JFileChooser();
				int returnVal = chooser.showOpenDialog(null);
				if(returnVal == JFileChooser.APPROVE_OPTION) {
					file = chooser.getSelectedFile();
					break;
				}
			}
		}
		
		JGLGameProperties props = new JGLGameProperties((!DEBUGGING) ? file.getName() : "Debugging", 800, 400);
		JGLGame game = new Main(props);
		JGLGameController.startGame(game, props);
	}
	
	public boolean test00E0() {
		reset();
		System.out.println("Test 0x00E0");
		opcode = 0x00E0;
		cycle();
		for(int i = 0; i < gfx.length; i++) {
			if(gfx[i] != 0) {
				return false;
			}
		}
		
		return true;
	}
	
	public boolean test00EE() {
		reset();
		System.out.println("Test 0x00EE");
		pushToStack(0xDEA);
		opcode = 0x00EE;
		cycle();
		return pc == 0xDEA;
	}
	
	public boolean test1NNN() {
		reset();
		System.out.println("Test 0x1NNN");
		opcode = 0x1EEF;
		cycle();
		return pc == 0x0EEF;
	}
	
	public boolean test2NNN() {
		reset();
		System.out.println("Test 0x2NNN");
		opcode = 0x2EEF;
		cycle();
		return popFromStack() == 0x200;
	}
	
	public boolean test3XNN_1() {
		reset();
		System.out.println("Test 0x3XNN_1");
		opcode = 0x30FF;
		cycle();
		return pc == 0x202;
	}
	
	public boolean test3XNN_2() {
		reset();
		System.out.println("Test 0x3XNN_2");
		V[0] = 0xFF;
		opcode = 0x30FF;
		cycle();
		return pc == 0x204;
	}
	
	public boolean test4XNN_1() {
		reset();
		System.out.println("Test 0x4XNN_1");
		V[0] = 0xFF;
		opcode = 0x40FF;
		cycle();
		return pc == 0x202;
	}
	public boolean test4XNN_2() {
		reset();
		System.out.println("Test 0x4XNN_2");
		opcode = 0x40FF;
		cycle();
		return pc == 0x204;
	}
	
	public boolean test5XY0_1() {
		reset();
		System.out.println("Test 0x5XY0_1");
		V[0] = 0x01;
		V[1] = 0x00;
		opcode = 0x5010;
		cycle();
		return pc == 0x202;
	}
	public boolean test5XY0_2() {
		reset();
		System.out.println("Test 0x5XY0_2");
		V[0] = 0x01;
		V[1] = 0x01;
		opcode = 0x5010;
		cycle();
		return pc == 0x204;
	}
	
	public boolean test6XNN() {
		reset();
		System.out.println("Test 0x6XNN");
		opcode = 0x6A10;
		cycle();
		return V[0xA] == 0x10;
	}
	
	public boolean test7XNN() {
		reset();
		System.out.println("Test 0x7XNN");
		V[0xA] = 0x10;
		opcode = 0x7A10;
		cycle();
		return V[0xA] == (0x10 + 0x10);
	}
	
	public boolean testDXYN() {
		reset();
		System.out.println("Test 0xDXYN");
		V[0] = 0;
		V[1] = 4;
		I = 0;
		write(I, 0b10011001);
		opcode = 0xD011;
		cycle();
		
		printScreen();
		
		for(int i = 256; i < 264; i++) {
			System.out.println("gfx[" + i + "] = " + gfx[i]);
		}
		
		return gfx[256] == 1
				&& gfx[257] == 0
				&& gfx[258] == 0
				&& gfx[259] == 1
				&& gfx[260] == 1
				&& gfx[261] == 0
				&& gfx[262] == 0
				&& gfx[263] == 1;
	}
	
	public void printScreen() {
		for(int y = 0; y < 32; y++) {
			for(int x = 0; x < 64; x++) {
				System.out.print(gfx[y * 64 + x]);
			}
			System.out.println();
		}
	}
}
