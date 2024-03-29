/*
 *  Copyright 2012 Peter Karich info@jetsli.de
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package de.jetsli.graph.coll;

import java.util.BitSet;

/**
 * @author Peter Karich
 */
public class MyBitSetImpl extends BitSet implements MyBitSet {

    public MyBitSetImpl() {
    }

    public MyBitSetImpl(int nbits) {
        super(nbits);
    }

    @Override
    public boolean contains(int index) {
        return super.get(index);
    }

    @Override
    public void add(int index) {
        super.set(index);
    }

    @Override
    public int getCardinality() {
        return super.cardinality();
    }        

    @Override
    public void ensureCapacity(int size) {
    }

    @Override
    public int next(int index) {
        return super.nextSetBit(index);
    }
}
