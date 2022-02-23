package com.rockbite.bongo.engine.render;

import com.badlogic.gdx.utils.Array;

public class ShaderFlags {


    Array<ShaderFlag> shaderFlagArray = new Array<>();
    int packedMask;
    StringBuilder prependString = new StringBuilder();

    public class ShaderFlag {
        String directive;
        int bitMask;
        boolean enabled;

        public ShaderFlag (String directive, int mask) {
            this.directive = directive;
            this.bitMask = mask;
            this.enabled = true;
        }

        public void setEnabled (boolean enabled) {
            boolean changed = false;
            if (enabled != this.enabled) {
                changed = true;
            }
            this.enabled = enabled;
            if (changed) {
                calculateMask();
            }
        }

        public boolean isEnabled () {
            return enabled;
        }
    }

    public ShaderFlag addFlag (String directive, int mask) {
        ShaderFlag flag = new ShaderFlag(directive, mask);
        shaderFlagArray.add(flag);
        calculateMask();
        return flag;
    }

    private void calculateMask () {
        packedMask = 0;
        prependString.setLength(0);
        for (ShaderFlag shaderFlag : shaderFlagArray) {
            if (!shaderFlag.enabled) continue;
            packedMask |= shaderFlag.bitMask;

            prependString.append("#define ");
            prependString.append(shaderFlag.directive);
            prependString.append("\n");
        }
    }

    public int getPackedMask () {
        return packedMask;
    }


    public CharSequence getPrepend () {
        return prependString;
    }
}
