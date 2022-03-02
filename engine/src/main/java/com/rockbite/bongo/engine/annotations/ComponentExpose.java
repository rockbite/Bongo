package com.rockbite.bongo.engine.annotations;

import lombok.Getter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = ElementType.FIELD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface ComponentExpose {

	ComponentExposeFlavour flavour () default ComponentExposeFlavour.NONE;

}
