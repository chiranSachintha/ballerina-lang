/*
*  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.wso2.ballerinalang.compiler;

import org.wso2.ballerinalang.compiler.util.Names;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

/**
 * {@code TypeSignatureReader} reads the type signature and creates type.
 * 
 * @param <T> Type of the type to be created from the signature.
 * 
 * @since 0.975.0
 */
public class TypeSignatureReader<T> {

    public int createBTypeFromSig(TypeCreator<T> typeCreator, char[] chars, int index, Stack<T> typeStack) {
        int nameIndex;
        char typeChar = chars[index];
        switch (typeChar) {
            case 'I':
            case 'W':
            case 'F':
            case 'L':
            case 'S':
            case 'B':
            case 'Y':
            case 'A':
            case 'N':
            case 'K':
                typeStack.push(typeCreator.getBasicType(typeChar));
                return index + 1;
            case 'R':
                index++;
                nameIndex = index;
                while (chars[nameIndex] != ';') {
                    nameIndex++;
                }
                String typeName = new String(Arrays.copyOfRange(chars, index, nameIndex));
                typeStack.push(typeCreator.getBuiltinRefType(typeName));
                return nameIndex + 1;
            case 'C':
            case 'T':
            case 'X':
            case 'D':
            case 'G':
            case 'Z':
            case 'E':
                nameIndex = index;
                while (chars[nameIndex] != ';') {
                    nameIndex++;
                }
                String name = new String(Arrays.copyOfRange(chars, index, nameIndex + 1));
                T type = getBTypeFromDescriptor(typeCreator, name);
                typeStack.push(type);
                return nameIndex + 1;
            case '[':
                int endIndex = index + 1;
                int j = index + 1;
                while (chars[j] != ';') {
                    endIndex++;
                    j++;
                }

                int size = Integer.parseInt(String.valueOf(Arrays.copyOfRange(chars, index + 1, endIndex)));
                index = createBTypeFromSig(typeCreator, chars, endIndex + 1, typeStack);
                T elemType = typeStack.pop();
                typeStack.push(typeCreator.getArrayType(elemType, size));

                return index;
            case 'M':
            case 'H':
            case 'Q':
                index = createBTypeFromSig(typeCreator, chars, index + 1, typeStack);
                T constraintType = typeStack.pop();
                typeStack.push(typeCreator.getConstrainedType(typeChar, constraintType));
                return index;
            case 'U':
                index++;
                index = createFunctionType(typeCreator, chars, index, typeStack);
                return index + 1;
            case 'O':
            case 'P':
                index++;
                nameIndex = index;
                while (chars[nameIndex] != ';') {
                    nameIndex++;
                }
                List<T> memberTypes = new ArrayList<>();
                int memberCount = Integer.parseInt(new String(Arrays.copyOfRange(chars, index, nameIndex)));
                index = nameIndex;
                for (int i = 0; i < memberCount; i++) {
                    index = createBTypeFromSig(typeCreator, chars, index + 1, typeStack) - 1;
                    memberTypes.add(typeStack.pop());
                }

                typeStack.push(typeCreator.getCollectionType(typeChar, memberTypes));
                return index + 1;
            default:
                throw new IllegalArgumentException("unsupported base type char: " + typeChar);
        }
    }

    public T getBTypeFromDescriptor(TypeCreator<T> typeCreator, String desc) {
        char ch = desc.charAt(0);
        switch (ch) {
            case 'I':
            case 'W':
            case 'F':
            case 'L':
            case 'S':
            case 'B':
            case 'Y':
            case 'A':
            case 'N':
            case 'K':
                return typeCreator.getBasicType(ch);
            case 'R':
                String typeName = desc.substring(1, desc.length() - 1);
                return typeCreator.getBuiltinRefType(typeName);
            case 'M':
            case 'H':
            case 'Q':
                T constraintType = getBTypeFromDescriptor(typeCreator, desc.substring(1));
                return typeCreator.getConstrainedType(ch, constraintType);
            case 'C':
            case 'T':
            case 'X':
            case 'Z':
            case 'G':
            case 'D':
            case 'E':
                typeName = desc.substring(1, desc.length() - 1);
                String[] parts = typeName.split(":");

                if (parts.length == 1) {
                    if (ch == 'D') {
                        return typeCreator.getConstrainedType(ch, null);
                    }
                }

                String pkgPath;
                String name;
                if (parts.length == 2) {
                    pkgPath = parts[0];
                    name = parts[1];
                } else {
                    pkgPath = String.join(Names.VERSION_SEPARATOR.value, parts[0], parts[1]);
                    name = parts[2];
                }

                constraintType = typeCreator.getRefType(ch, pkgPath, name);
                return typeCreator.getConstrainedType(ch, constraintType);
            case '[':
                int index = 1;
                char[] size = null;
                if (desc.contains(";")) {
                    index = desc.indexOf(";");
                    size = new char[index - 1];
                    desc.getChars(1, index, size, 0);
                    index++;
                }
                T elemType = getBTypeFromDescriptor(typeCreator, desc.substring(index));
                return typeCreator.getArrayType(elemType, Integer.parseInt(String.valueOf(size)));

            case 'U':
            case 'O':
            case 'P':
                Stack<T> typeStack = new Stack<>();
                createBTypeFromSig(typeCreator, desc.toCharArray(), 0, typeStack);
                return typeStack.pop();
            default:
                throw new IllegalArgumentException("unsupported base type char: " + ch);
        }
    }

    public int createFunctionType(TypeCreator<T> typeCreator, char[] chars, int index, Stack<T> typeStack) {
        // Skip the first parenthesis
        index++;

        // Read function parameters
        Stack<T> funcParamsStack = new Stack<>();
        while (chars[index] != ')' || chars[index + 1] != '(') {
            index = createBTypeFromSig(typeCreator, chars, index, funcParamsStack);
        }

        // Read function return type.
        // Skip the two parenthesis ')(', which separate params and return params
        index += 2;
        T retType;
        if (chars[index] == ')') {
            retType = null;
        } else {
            Stack<T> returnParamsStack = new Stack<>();
            index = createBTypeFromSig(typeCreator, chars, index, returnParamsStack);
            retType = returnParamsStack.pop();
        }

        typeStack.push(typeCreator.getFunctionType(funcParamsStack, retType));
        return index;
    }
}
