/*******************************************************************************
 * Copyright 2012 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
//
// Generated by JTB 1.3.2
//

package org.ohmage.config.grammar.syntaxtree;

/**
 * The interface which NodeList, NodeListOptional, and NodeSequence
 * implement.
 */
public interface NodeListInterface extends Node {
	public void addNode(Node n);
	public Node elementAt(int i);
	public java.util.Enumeration<Node> elements();
	public int size();

	public void accept(org.ohmage.config.grammar.visitor.Visitor v);
	public <R,A> R accept(org.ohmage.config.grammar.visitor.GJVisitor<R,A> v, A argu);
	public <R> R accept(org.ohmage.config.grammar.visitor.GJNoArguVisitor<R> v);
	public <A> void accept(org.ohmage.config.grammar.visitor.GJVoidVisitor<A> v, A argu);
}

