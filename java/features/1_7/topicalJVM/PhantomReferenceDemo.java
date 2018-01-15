package topicalJVM;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;

/**
 * 17-8-17
 */
public class PhantomReferenceDemo {

    private static class TestObject {
        @Override
        protected void finalize() throws Throwable {
            System.out.println("finalize方法被调用");
            super.finalize();
        }

        public void phantomReferenceQueue() {
            ReferenceQueue<TestObject> queue = new ReferenceQueue<>();
            TestObject obj = new TestObject();
            PhantomReference<TestObject> phantomRef = new PhantomReference<>(obj, queue);
            obj = null;
            Reference<? extends TestObject> ref = null;



        }
    }
}
