/**
 * Copyright (c) 2018, hiwepy (https://github.com/hiwepy).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.baomidou.mybatisplus.enhance.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;

/**
 * Database integer boolean enumeration.
 *
 * <p>Maps {@code false/true} to {@code 0/1} respectively via the MyBatis-Plus {@link IEnum} contract,
 * enabling seamless persistence of Java boolean semantics into integer database columns.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
@Getter
public enum BooleanEnum implements IEnum<Integer> {

    /**
     * Database storage value {@code 0}, representing logical false.
     */
    IS_FALSE(false, "否"),

    /**
     * Database storage value {@code 1}, representing logical true.
     */
    IS_TRUE(true, "是");

    /**
     * Java boolean semantic value.
     */
    private final boolean booleanValue;

    /**
     * Chinese display name for the enumeration.
     */
    private final String nameCn;

    /**
     * Creates a database integer boolean enum entry.
     *
     * @param booleanValue Java boolean semantic value
     * @param nameCn       Chinese display name
     */
    BooleanEnum(boolean booleanValue, String nameCn) {
        this.booleanValue = booleanValue;
        this.nameCn = nameCn;
    }

    /**
     * Resolves the enum entry from a database integer value.
     *
     * @param value database storage value; only {@code 0} and {@code 1} are supported
     * @return the corresponding boolean enum entry
     * @throws IllegalArgumentException if the value is not {@code 0} or {@code 1}
     */
    public static BooleanEnum valueOf(int value) {
        if (value == IS_FALSE.getValue()) {
            return IS_FALSE;
        }
        if (value == IS_TRUE.getValue()) {
            return IS_TRUE;
        }
        throw new IllegalArgumentException("Unsupported boolean database value: " + value);
    }

    /**
     * Returns the MyBatis-Plus persistence value for this enum entry.
     *
     * @return {@code 0} for {@code false}, {@code 1} for {@code true}
     */
    @Override
    public Integer getValue() {
        return booleanValue ? 1 : 0;
    }
}
