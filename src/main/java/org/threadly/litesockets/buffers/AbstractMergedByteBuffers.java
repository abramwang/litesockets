package org.threadly.litesockets.buffers;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.threadly.util.ArgumentVerifier;

/**
 * This class is used to combine multiple ByteBuffers into 1 simplish to use interface.
 * It provides most of the features of a single ByteBuffer, but with the ability to perform those 
 * operations spanning many ByteBuffers.
 * 
 * The idea here is to keep from having to copy around and merge ByteBuffers as much as possible. 
 * 
 * NOTE: This is not threadSafe.  It should only be accessed by 1 thread at a time.
 * 
 */
@SuppressWarnings("deprecation")
public abstract class AbstractMergedByteBuffers implements MergedByteBuffers {

  protected final boolean markReadOnly;

  public AbstractMergedByteBuffers() {
    this(true);
  }

  public AbstractMergedByteBuffers(boolean readOnly) {
    this.markReadOnly = readOnly;
  }
  
  public AbstractMergedByteBuffers(boolean readOnly, ByteBuffer ...bbs) {
    this.markReadOnly = readOnly;
    add(bbs);
  }
  
  protected abstract void doAppend(final ByteBuffer bb);
  protected abstract void addToFront(final ByteBuffer bb);
  protected abstract byte get(int pos);
  public abstract AbstractMergedByteBuffers duplicate();
  public abstract AbstractMergedByteBuffers duplicateAndClean();
  public abstract byte get();
  public abstract void get(byte[] destBytes, int start, int length);
  public abstract int nextBufferSize();
  public abstract ByteBuffer popBuffer(); 
  public abstract int remaining();
  public abstract boolean hasRemaining();
  public abstract ByteBuffer pullBuffer(final int size);
  public abstract void discard(final int size); 
  public abstract long getTotalConsumedBytes();
  public abstract boolean isAppendable(); 

  @Override
  public void add(final byte[] ...bas) {
    for(byte[] ba: bas) {
      if(ba.length > 0) {
        doAppend(ByteBuffer.wrap(ba));
      }
    }
  }
  
  @Override
  public void add(final ByteBuffer ...buffers) {
    for(ByteBuffer buffer: buffers) {
      if(buffer.hasRemaining()) {
        doAppend(buffer.slice());
      }
    }
  }

  @Override
  public void add(final MergedByteBuffers ...mbbs) {
    for(MergedByteBuffers mbb: mbbs) {
      while(mbb.remaining() > 0) {
        ByteBuffer bb = mbb.popBuffer();
        if(bb.hasRemaining()) {
          doAppend(bb);
        }
      }
    }
  }
  
  @Override
  public void get(final byte[] destBytes) {
    ArgumentVerifier.assertNotNull(destBytes, "byte[]");
    get(destBytes, 0, destBytes.length);
  }
  
  @Override
  public int indexOf(final String pattern) {
    return indexOf(pattern, Charset.forName("US-ASCII"), 0);
  }
  

  @Override
  public int indexOf(String pattern, int fromPosition) {
    return indexOf(pattern, Charset.forName("US-ASCII"), fromPosition);
  }

  @Override
  public int indexOf(final String pattern, final Charset charSet) {
    ArgumentVerifier.assertNotNull(pattern, "String");
    return indexOf(pattern.getBytes(charSet), 0);
  }
  

  @Override
  public int indexOf(String pattern, Charset charSet, int fromPosition) {
    return indexOf(pattern.getBytes(charSet), fromPosition);
  }
  
  @Override
  public int indexOf(final byte[] pattern) {
    return findIndexOf(this, pattern, 0);
  }
  
  public int indexOf(final byte[] pattern, int fromPosition) {
    return findIndexOf(this, pattern, fromPosition);
  }

  @Override
  public short getUnsignedByte() {
    return (short)(get() & UNSIGNED_BYTE_MASK);
  }

  @Override
  public int getUnsignedShort() {
    return getShort() & UNSIGNED_SHORT_MASK;
  }

  @Override
  public short getShort() {
    if (remaining() < BYTES_IN_SHORT) {
      throw new BufferUnderflowException();
    }
    return pullBuffer(BYTES_IN_SHORT).getShort();
  }

  @Override
  public int getInt() {
    if (remaining() < BYTES_IN_INT) {
      throw new BufferUnderflowException();
    }
    return pullBuffer(BYTES_IN_INT).getInt();
  }

  @Override
  public long getUnsignedInt() {    
    return getInt() & UNSIGNED_INT_MASK;
  }

  @Override
  public long getLong() {
    if (remaining() < BYTES_IN_LONG) {
      throw new BufferUnderflowException();
    }
    return pullBuffer(BYTES_IN_LONG).getLong();
  }

  public String getAsString(final int size) {
    return getAsString(size, Charset.forName("US-ASCII"));
  }

  public String getAsString(final int size, final Charset charSet) {
    ArgumentVerifier.assertNotNegative(size, "size");
    final byte[] ba = new byte[size];
    get(ba);
    return new String(ba, charSet);
  }

  @Override
  public String toString() {
    return "MergedByteBuffer size:"+remaining()+": consumed:"+getTotalConsumedBytes();
  }
  
  protected static int findIndexOf(AbstractMergedByteBuffers abb, final byte[] pattern, int fromPosition) {
    ArgumentVerifier.assertNotNull(pattern, "byte[]");
    final int total = abb.remaining();
    if(total < fromPosition){
      return -1;
    }

    int patPos = 0;
    int bufPos = fromPosition;


    byte[] prevMatched = new byte[pattern.length];
    while(total-bufPos >= pattern.length-patPos) {
      

      prevMatched[patPos] = abb.get(bufPos+patPos);
      if(pattern[patPos] == prevMatched[patPos]) {
        if(patPos == pattern.length-1) {
          return bufPos;
        }
        patPos++;
      } else {
        bufPos++;
        patPos = 0;
      }
    }
    return -1;
  }
  

  @Override
  public int nextPopSize() {
    return this.nextBufferSize();
  }

  @Override
  public ByteBuffer pop() {
    return this.popBuffer();
  }

  @Override
  public ByteBuffer pull(int size) {
    return this.pullBuffer(size);
  }
  
  @Override
  public MergedByteBuffers copy() {
    return this.duplicate();
  }
}
