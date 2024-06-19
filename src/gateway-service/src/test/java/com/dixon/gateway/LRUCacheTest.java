package com.dixon.gateway;

public class LRUCacheTest {
    public static void main(String args[]) {
        Data[] data = new Data[] {
                Data.builder().name("Tux").quantity(10).price(9.99).build(),
                Data.builder().name("Whale").quantity(20).price(19.99).build(),
                Data.builder().name("Elephant").quantity(30).price(29.99).build(),
                Data.builder().name("Dolphin").quantity(40).price(39.99).build(),
                Data.builder().name("Fox").quantity(50).price(49.99).build(),
                Data.builder().name("Python").quantity(60).price(59.99).build(),
                Data.builder().name("Shark").quantity(70).price(69.99).build(),
                Data.builder().name("Panda").quantity(80).price(79.99).build(),
                Data.builder().name("Duck").quantity(90).price(89.99).build(),
                Data.builder().name("Turtle").quantity(100).price(99.99).build(),
        };


        // Create cache
        LRUCache cache = new LRUCache(5);

        // Test putting items in cache
        System.out.println("TESTING PUTTING ITEMS:");
        for (int i = 0; i < 10; i++) {
            cache.put(data[i]);
            System.out.println(cache);
        }
        System.out.println();


        // Test getting items
        System.out.println("TESTING GETTING ITEMS:");
        // Test getting first item in cache
        Data firstItem = cache.get("Turtle");
        System.out.println("First Item: " + firstItem.toString());
        System.out.println(cache);
        // Test getting last item in cache
        Data lastItem = cache.get("Python");
        System.out.println("Last Item: " + lastItem.toString());
        System.out.println(cache);
        // Test getting middle item in cache
        Data middleItem = cache.get("Duck");
        System.out.println("Middle Item: " + middleItem.toString());
        System.out.println(cache);
        System.out.println();

        // Test invalidating items
        System.out.println("TESTING INVALIDATING ITEMS:");
        // Test invalidating first item in cache
        System.out.println("Invalidating Duck (first item)");
        cache.invalidate("Duck");
        System.out.println(cache);
        // Test invalidating last item in cache
        System.out.println("Invalidating Shark (last item)");
        cache.invalidate("Shark");
        System.out.println(cache);
        // Test invalidating middle item in cache
        System.out.println("Invalidating Turtle (middle item)");
        cache.invalidate("Turtle");
        System.out.println(cache);
        // Test invalidating rest of items to empty cache
        System.out.println("Invalidating remaining items");
        cache.invalidate("Panda");
        System.out.println(cache);
        cache.invalidate("Python");
        System.out.println(cache);
        System.out.println();

        // Test putting items into again cache
        System.out.println("TESTING PUTTING ITEMS BACK IN TO CACHE:");
        for (int i = 0; i < 10; i++) {
            cache.put(data[i]);
            System.out.println(cache);
        }
        System.out.println();

        // Test updating item quantity of item already in cache
        System.out.println("TESTING UPDATING ITEM ALREADY IN CACHE:");
        Data new_data = Data.builder().name("Duck").quantity(89).price(89.99).build();
        cache.put(new_data);
        System.out.println(cache);
    }
}
