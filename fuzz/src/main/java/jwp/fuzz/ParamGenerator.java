package jwp.fuzz;

import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.*;

public interface ParamGenerator<T> extends AutoCloseable {

  // Guaranteed to be iterated in a single thread.
  Iterator<T> iterator();

  boolean isInfinite();

  // Note, myParam is not necessarily the same type as result.params[myParamIndex] because
  // it may be mapped.
  default void onComplete(ExecutionResult result, int myParamIndex, T myParam) { }

  @Override
  default void close() throws Exception { }

  default <U> ParamGenerator<U> mapNotNull(Function<T, U> fnTo, Function<U, T> fnFrom) {
    final ParamGenerator<T> self = this;
    return new ParamGenerator<U>() {
      @Override
      public Iterator<U> iterator() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
          self.iterator(), Spliterator.ORDERED), false).map(fnTo).filter(Objects::nonNull).iterator();
      }

      @Override
      public boolean isInfinite() { return self.isInfinite(); }

      @Override
      public void onComplete(ExecutionResult result, int myParamIndex, U myParam) {
        self.onComplete(result, myParamIndex, fnFrom.apply(myParam));
      }

      @Override
      public void close() throws Exception { self.close(); }
    };
  }

  static ParamGenerator<?> suggested(Class<?> cls) {
    if (cls == Boolean.TYPE) return of(true, false);
    if (cls == Boolean.class) return of(null, true, false);
    if (cls == Byte.TYPE) return of(interestingBytes());
    if (cls == Byte.class) return of(Stream.concat(Stream.of((Integer) null), interestingBytes().boxed()));
    if (cls == Short.TYPE) return of(interestingShorts());
    if (cls == Short.class) return of(Stream.concat(Stream.of((Integer) null), interestingShorts().boxed()));
    if (cls == Integer.TYPE) return of(interestingInts());
    if (cls == Integer.class) return of(Stream.concat(Stream.of((Integer) null), interestingInts().boxed()));
    if (cls == Long.TYPE) return of(interestingLongs());
    if (cls == Long.class) return of(Stream.concat(Stream.of((Long) null), interestingLongs().boxed()));
    if (cls == Float.TYPE) return of(interestingFloats());
    if (cls == Float.class) return of(Stream.concat(Stream.of((Float) null), interestingFloats().boxed()));
    if (cls == Double.TYPE) return of(interestingDoubles());
    if (cls == Double.class) return of(Stream.concat(Stream.of((Double) null), interestingDoubles().boxed()));
    if (cls == Character.TYPE) throw new UnsupportedOperationException("TODO");
    if (cls == Character.class) throw new UnsupportedOperationException("TODO");
    throw new IllegalArgumentException("No suggested generator for " + cls);
  }

  static <T, S extends BaseStream<T, S>> ParamGenerator<T> ofFixed(BaseStream<T, S> stream) {
    return new ParamGenerator<T>() {
      @Override
      public Iterator<T> iterator() { return stream.iterator(); }

      @Override
      public boolean isInfinite() { return false; }

      @Override
      public void close() { stream.close(); }
    };
  }

  @SafeVarargs
  static <T> ParamGenerator<T> of(T... items) { return ofFixed(Stream.of(items)); }

  static IntStream interestingBytes() {
    return IntStream.concat(
        IntStream.of(Byte.MIN_VALUE, 64, 100, Byte.MAX_VALUE),
        IntStream.rangeClosed(-35, 35)
    );
  }

  static IntStream interestingShorts() {
    return IntStream.concat(
        interestingBytes(),
        IntStream.of(Short.MIN_VALUE, -129, 128, 255, 256, 512, 1000, 1024, 4096, Short.MAX_VALUE)
    );
  }

  static IntStream interestingInts() {
    return IntStream.concat(
        interestingShorts(),
        IntStream.of(Integer.MIN_VALUE, -100663046, -32769, 32768, 65535, 65536, 100663045, Integer.MAX_VALUE)
    );
  }

  static LongStream interestingLongs() {
    return LongStream.concat(
        interestingInts().asLongStream(),
        LongStream.of(Long.MIN_VALUE, Long.MAX_VALUE)
    );
  }

  static DoubleStream interestingFloats() {
    return DoubleStream.concat(
        interestingLongs().asDoubleStream(),
        DoubleStream.of(Float.MIN_NORMAL, Float.MIN_VALUE, Float.MAX_VALUE,
            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NaN)
    );
  }

  static DoubleStream interestingDoubles() {
    return DoubleStream.concat(
        interestingFloats(),
        DoubleStream.of(Double.MIN_NORMAL, Double.MIN_VALUE, Double.MAX_VALUE)
    );
  }
}
