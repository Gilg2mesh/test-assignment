/*
 * Copyright (c) 2014, NTUU KPI, Computer systems department and/or its affiliates. All rights reserved.
 * NTUU KPI PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */

package ua.kpi.comsys.test2.implementation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.UncheckedIOException;

import java.math.BigInteger;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import ua.kpi.comsys.test2.NumberList;

/**
 * Custom implementation of NumberList interface (module 2 assignment).
 *
 * <p><b>Author:</b> Якимов Віктор, ІК-32, № заліковки 14309476</p>
 *
 * <p><b>Variant (by last 4 digits = 9476):</b>
 * C3=2 -> circular doubly linked list,
 * C5=1 -> base-3,
 * extra base for changeScale: (C5+1) mod 5 = 2 -> base-8,
 * C7=5 -> AND (bitwise AND on numbers).
 * </p>
 */
public class NumberListImpl implements NumberList {

    // ===== Internal structure: circular doubly linked list (C3=2) =====
    private static final class Node {
        byte value;  // digit
        Node next;
        Node prev;

        Node(byte value) {
            this.value = value;
        }
    }

    private Node head;   // most significant digit (MSD)
    private int size;
    private int base;    // current scale, for this variant: 3 (main), 8 (additional)

    // ===== Helpers =====

    private static int mod(int a, int m) {
        int r = a % m;
        return r < 0 ? r + m : r;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException("index=" + index + ", size=" + size);
    }

    private void checkIndexAdd(int index) {
        if (index < 0 || index > size)
            throw new IndexOutOfBoundsException("index=" + index + ", size=" + size);
    }

    private void ensureDigit(Byte e) {
        if (e == null) throw new NullPointerException("null digits are not allowed");
        int v = e.intValue();
        if (v < 0 || v >= base)
            throw new IllegalArgumentException("Digit " + v + " out of range for base " + base);
    }

    private Node nodeAt(int index) {
        checkIndex(index);
        // bidirectional traversal
        if (index <= size / 2) {
            Node cur = head;
            for (int i = 0; i < index; i++) cur = cur.next;
            return cur;
        } else {
            Node cur = head.prev; // tail
            for (int i = size - 1; i > index; i--) cur = cur.prev;
            return cur;
        }
    }

    private void linkAsOnly(Node n) {
        n.next = n;
        n.prev = n;
        head = n;
        size = 1;
    }

    private void append(byte v) {
        Node n = new Node(v);
        if (size == 0) {
            linkAsOnly(n);
            return;
        }
        Node tail = head.prev;
        n.next = head;
        n.prev = tail;
        tail.next = n;
        head.prev = n;
        size++;
    }

    private void insertAt(int index, byte v) {
        if (index == size) { // append
            append(v);
            return;
        }
        Node cur = nodeAt(index);
        Node n = new Node(v);

        Node prev = cur.prev;
        n.next = cur;
        n.prev = prev;
        prev.next = n;
        cur.prev = n;

        if (index == 0) head = n;
        size++;
    }

    private byte removeNode(Node cur) {
        byte old = cur.value;
        if (size == 1) {
            clear();
            return old;
        }
        Node prev = cur.prev;
        Node next = cur.next;
        prev.next = next;
        next.prev = prev;
        if (cur == head) head = next;
        size--;
        return old;
    }

    private static String readAllTrimmed(File file) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line.trim());
            return sb.toString().trim();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void writeString(File file, String s) {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)))) {
            bw.write(s);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static BigInteger parseDecimalString(String value) {
        if (value == null) throw new NullPointerException("value is null");
        String v = value.trim();
        if (v.isEmpty()) return BigInteger.ZERO;
        if (!v.matches("\\d+")) throw new IllegalArgumentException("Decimal string expected, got: " + value);
        return new BigInteger(v);
    }

    private BigInteger toBigIntegerDecimal() {
        // empty list -> 0
        BigInteger result = BigInteger.ZERO;
        Node cur = head;
        for (int i = 0; i < size; i++) {
            int digit = cur.value & 0xFF;
            result = result.multiply(BigInteger.valueOf(base)).add(BigInteger.valueOf(digit));
            cur = cur.next;
        }
        return result;
    }

    private void setFromDecimal(BigInteger dec, int targetBase) {
        clear();
        this.base = targetBase;

        if (dec.signum() < 0) throw new IllegalArgumentException("Only positive numbers allowed");
        if (dec.equals(BigInteger.ZERO)) return; // empty list allowed

        BigInteger b = BigInteger.valueOf(targetBase);

        // collect digits in reverse using StringBuilder (not a java collection of digits)
        StringBuilder rev = new StringBuilder();
        BigInteger x = dec;
        while (x.compareTo(BigInteger.ZERO) > 0) {
            BigInteger[] qr = x.divideAndRemainder(b);
            int digit = qr[1].intValueExact();
            rev.append((char) ('A' + digit)); // temporary token
            x = qr[0];
        }

        // reverse to build MSB -> LSB
        for (int i = rev.length() - 1; i >= 0; i--) {
            int digit = rev.charAt(i) - 'A';
            append((byte) digit);
        }
    }

    // ===== Constructors =====

    /**
     * Default constructor. Returns empty NumberListImpl
     */
    public NumberListImpl() {
        this.base = 3; // C5=1 => base-3
        this.head = null;
        this.size = 0;
    }

    /**
     * Constructs new NumberListImpl by decimal number from file.
     *
     * @param file - file where number is stored.
     */
    public NumberListImpl(File file) {
        this();
        String s = readAllTrimmed(file);
        BigInteger dec = parseDecimalString(s);
        setFromDecimal(dec, 3);
    }

    /**
     * Constructs new NumberListImpl by decimal number in string notation.
     *
     * @param value - number in string notation.
     */
    public NumberListImpl(String value) {
        this();
        BigInteger dec = parseDecimalString(value);
        setFromDecimal(dec, 3);
    }

    /**
     * Saves the number, stored in the list, into specified file in decimal scale.
     *
     * @param file - file where number has to be stored.
     */
    public void saveList(File file) {
        writeString(file, toDecimalString());
    }

    /**
     * Returns student's record book number, which has 4 decimal digits.
     *
     * @return student's record book number.
     */
    public static int getRecordBookNumber() {
        // last 4 digits of 14309476
        return 9476;
    }

    /**
     * Returns new NumberListImpl which represents the same number in other scale of notation.
     * For this variant: base-3 -> base-8.
     *
     * @return NumberListImpl in other scale of notation.
     */
    public NumberListImpl changeScale() {
        BigInteger dec = toBigIntegerDecimal();
        NumberListImpl res = new NumberListImpl();
        res.setFromDecimal(dec, 8);
        return res;
    }

    /**
     * Additional operation (C7=5): AND (bitwise AND on numbers).
     * Operands must not change.
     *
     * @param arg - second argument
     * @return result list
     */
    public NumberListImpl additionalOperation(NumberList arg) {
        if (arg == null) throw new NullPointerException("arg is null");

        BigInteger a = this.toBigIntegerDecimal();
        BigInteger b;

        if (arg instanceof NumberListImpl) {
            b = ((NumberListImpl) arg).toBigIntegerDecimal();
        } else {
            // fallback: treat arg as digits list in the same base (best effort)
            BigInteger acc = BigInteger.ZERO;
            for (Byte d : arg) {
                if (d == null) throw new NullPointerException("null digit in arg");
                int digit = d & 0xFF;
                acc = acc.multiply(BigInteger.valueOf(this.base)).add(BigInteger.valueOf(digit));
            }
            b = acc;
        }

        BigInteger r = a.and(b);

        NumberListImpl res = new NumberListImpl();
        res.setFromDecimal(r, this.base); // keep same base as current list
        return res;
    }

    /**
     * Returns string representation of number, stored in the list in decimal scale.
     *
     * @return string representation in decimal scale.
     */
    public String toDecimalString() {
        return toBigIntegerDecimal().toString();
    }

    // ===== Object contract =====

    @Override
    public String toString() {
        // List-style: [d1, d2, ...] where d1 is MSD
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        Node cur = head;
        for (int i = 0; i < size; i++) {
            sb.append(cur.value & 0xFF);
            if (i + 1 < size) sb.append(", ");
            cur = cur.next;
        }
        sb.append(']');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof List)) return false;

        List<?> other = (List<?>) o;
        if (other.size() != this.size) return false;

        Iterator<?> it = other.iterator();
        Node cur = head;
        for (int i = 0; i < size; i++) {
            Object v = it.next();
            if (!(v instanceof Byte)) return false;
            if (((Byte) v).byteValue() != cur.value) return false;
            cur = cur.next;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h = 1;
        Node cur = head;
        for (int i = 0; i < size; i++) {
            h = 31 * h + (cur.value & 0xFF);
            cur = cur.next;
        }
        return h;
    }

    // ===== List basics =====

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean contains(Object o) {
        if (!(o instanceof Byte)) return false;
        byte v = (Byte) o;
        Node cur = head;
        for (int i = 0; i < size; i++) {
            if (cur.value == v) return true;
            cur = cur.next;
        }
        return false;
    }

    @Override
    public Iterator<Byte> iterator() {
        return new Iterator<Byte>() {
            private int seen = 0;
            private Node cur = head;
            private Node lastReturned = null;

            @Override
            public boolean hasNext() {
                return seen < size;
            }

            @Override
            public Byte next() {
                if (!hasNext()) throw new NoSuchElementException();
                lastReturned = cur;
                byte v = cur.value;
                cur = cur.next;
                seen++;
                return v;
            }

            @Override
            public void remove() {
                if (lastReturned == null) throw new IllegalStateException();
                removeNode(lastReturned);
                seen--; // one removed from already-seen
                lastReturned = null;
            }
        };
    }

    @Override
    public Object[] toArray() {
        Object[] arr = new Object[size];
        Node cur = head;
        for (int i = 0; i < size; i++) {
            arr[i] = cur.value; // Byte autobox
            cur = cur.next;
        }
        return arr;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        // README says this method may be not implemented
        throw new UnsupportedOperationException("toArray(T[] a) is not required by assignment");
    }

    @Override
    public boolean add(Byte e) {
        ensureDigit(e);
        append(e.byteValue());
        return true;
    }

    @Override
    public boolean remove(Object o) {
        if (!(o instanceof Byte)) return false;
        byte v = (Byte) o;
        Node cur = head;
        for (int i = 0; i < size; i++) {
            if (cur.value == v) {
                removeNode(cur);
                return true;
            }
            cur = cur.next;
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object x : c) if (!contains(x)) return false;
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends Byte> c) {
        boolean changed = false;
        for (Byte b : c) {
            add(b);
            changed = true;
        }
        return changed;
    }

    @Override
    public boolean addAll(int index, Collection<? extends Byte> c) {
        checkIndexAdd(index);
        boolean changed = false;
        int i = index;
        for (Byte b : c) {
            ensureDigit(b);
            insertAt(i++, b.byteValue());
            changed = true;
        }
        return changed;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean changed = false;
        Iterator<Byte> it = iterator();
        while (it.hasNext()) {
            if (c.contains(it.next())) {
                it.remove();
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean changed = false;
        Iterator<Byte> it = iterator();
        while (it.hasNext()) {
            if (!c.contains(it.next())) {
                it.remove();
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public void clear() {
        head = null;
        size = 0;
    }

    @Override
    public Byte get(int index) {
        return nodeAt(index).value;
    }

    @Override
    public Byte set(int index, Byte element) {
        ensureDigit(element);
        Node n = nodeAt(index);
        byte old = n.value;
        n.value = element.byteValue();
        return old;
    }

    @Override
    public void add(int index, Byte element) {
        ensureDigit(element);
        checkIndexAdd(index);
        insertAt(index, element.byteValue());
    }

    @Override
    public Byte remove(int index) {
        Node n = nodeAt(index);
        return removeNode(n);
    }

    @Override
    public int indexOf(Object o) {
        if (!(o instanceof Byte)) return -1;
        byte v = (Byte) o;
        Node cur = head;
        for (int i = 0; i < size; i++) {
            if (cur.value == v) return i;
            cur = cur.next;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        if (!(o instanceof Byte)) return -1;
        byte v = (Byte) o;
        Node cur = head;
        int last = -1;
        for (int i = 0; i < size; i++) {
            if (cur.value == v) last = i;
            cur = cur.next;
        }
        return last;
    }

    @Override
    public ListIterator<Byte> listIterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator<Byte> listIterator(int index) {
        checkIndexAdd(index);
        return new ListIterator<Byte>() {
            private int pos = index;
            private int lastPos = -1;

            @Override public boolean hasNext() { return pos < size; }
            @Override public boolean hasPrevious() { return pos > 0; }
            @Override public int nextIndex() { return pos; }
            @Override public int previousIndex() { return pos - 1; }

            @Override
            public Byte next() {
                if (!hasNext()) throw new NoSuchElementException();
                Byte v = get(pos);
                lastPos = pos;
                pos++;
                return v;
            }

            @Override
            public Byte previous() {
                if (!hasPrevious()) throw new NoSuchElementException();
                pos--;
                Byte v = get(pos);
                lastPos = pos;
                return v;
            }

            @Override
            public void remove() {
                if (lastPos < 0) throw new IllegalStateException();
                NumberListImpl.this.remove(lastPos);
                if (lastPos < pos) pos--;
                lastPos = -1;
            }

            @Override
            public void set(Byte e) {
                if (lastPos < 0) throw new IllegalStateException();
                NumberListImpl.this.set(lastPos, e);
            }

            @Override
            public void add(Byte e) {
                NumberListImpl.this.add(pos, e);
                pos++;
                lastPos = -1;
            }
        };
    }

    @Override
    public List<Byte> subList(int fromIndex, int toIndex) {
        if (fromIndex < 0 || toIndex > size || fromIndex > toIndex)
            throw new IndexOutOfBoundsException("from=" + fromIndex + ", to=" + toIndex + ", size=" + size);

        NumberListImpl res = new NumberListImpl();
        res.base = this.base;
        for (int i = fromIndex; i < toIndex; i++) res.add(get(i));
        return res;
    }

    // ===== NumberList extra operations =====

    @Override
    public boolean swap(int index1, int index2) {
        if (index1 < 0 || index1 >= size || index2 < 0 || index2 >= size) return false;
        if (index1 == index2) return true;

        Node a = nodeAt(index1);
        Node b = nodeAt(index2);
        byte t = a.value;
        a.value = b.value;
        b.value = t;
        return true;
    }

    @Override
    public void sortAscending() {
        if (size < 2) return;
        for (int i = 0; i < size - 1; i++) {
            Node a = nodeAt(i);
            for (int j = i + 1; j < size; j++) {
                Node b = nodeAt(j);
                if ((a.value & 0xFF) > (b.value & 0xFF)) {
                    byte t = a.value; a.value = b.value; b.value = t;
                }
            }
        }
    }

    @Override
    public void sortDescending() {
        if (size < 2) return;
        for (int i = 0; i < size - 1; i++) {
            Node a = nodeAt(i);
            for (int j = i + 1; j < size; j++) {
                Node b = nodeAt(j);
                if ((a.value & 0xFF) < (b.value & 0xFF)) {
                    byte t = a.value; a.value = b.value; b.value = t;
                }
            }
        }
    }

    @Override
    public void shiftLeft() {
        if (size <= 1) return;
        head = head.next;
    }

    @Override
    public void shiftRight() {
        if (size <= 1) return;
        head = head.prev;
    }
}
