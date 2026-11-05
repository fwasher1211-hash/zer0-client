package net.minecraft.client.option; public class SimpleOption<T> { T v; public SimpleOption(T v){this.v=v;} public T getValue(){return v;} public void setValue(T v){this.v=v;} }
