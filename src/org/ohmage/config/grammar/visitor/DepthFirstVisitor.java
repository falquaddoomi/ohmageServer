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

package org.ohmage.config.grammar.visitor;
import java.util.Enumeration;

import org.ohmage.config.grammar.syntaxtree.Condition;
import org.ohmage.config.grammar.syntaxtree.Conjunction;
import org.ohmage.config.grammar.syntaxtree.Expr;
import org.ohmage.config.grammar.syntaxtree.Id;
import org.ohmage.config.grammar.syntaxtree.Node;
import org.ohmage.config.grammar.syntaxtree.NodeList;
import org.ohmage.config.grammar.syntaxtree.NodeListOptional;
import org.ohmage.config.grammar.syntaxtree.NodeOptional;
import org.ohmage.config.grammar.syntaxtree.NodeSequence;
import org.ohmage.config.grammar.syntaxtree.NodeToken;
import org.ohmage.config.grammar.syntaxtree.Sentence;
import org.ohmage.config.grammar.syntaxtree.SentencePrime;
import org.ohmage.config.grammar.syntaxtree.Start;
import org.ohmage.config.grammar.syntaxtree.Value;

/**
 * Provides default methods which visit each node in the tree in depth-first
 * order.  Your visitors may extend this class.
 */
public class DepthFirstVisitor implements Visitor {
	//
	// Auto class visitors--probably don't need to be overridden.
	//
	public void visit(NodeList n) {
		for ( Enumeration<Node> e = n.elements(); e.hasMoreElements(); )
			e.nextElement().accept(this);
	}
	
	public void visit(NodeListOptional n) {
		if ( n.present() )
			for ( Enumeration<Node> e = n.elements(); e.hasMoreElements(); )
				e.nextElement().accept(this);
	}

	public void visit(NodeOptional n) {
		if ( n.present() )
			n.node.accept(this);
	}

	public void visit(NodeSequence n) {
		for ( Enumeration<Node> e = n.elements(); e.hasMoreElements(); )
			e.nextElement().accept(this);
	}

	public void visit(NodeToken n) { }

	//
	// User-generated visitor methods below
	//

	/**
	 * f0 -> Sentence()
	 * f1 -> <EOF>
	 */
	public void visit(Start n) {
		n.f0.accept(this);
		n.f1.accept(this);
	}

	/**
	 * f0 -> Expr() SentencePrime()
	 *       | "(" Sentence() ")" SentencePrime()
	 */
	public void visit(Sentence n) {
		n.f0.accept(this);
	}

	/**
	 * f0 -> ( Conjunction() Sentence() SentencePrime() )?
	 */
	public void visit(SentencePrime n) {
		n.f0.accept(this);
	}

	/**
	 * f0 -> Id()
	 * f1 -> Condition()
	 * f2 -> Value()
	 */
	public void visit(Expr n) {
		n.f0.accept(this);
		n.f1.accept(this);
		n.f2.accept(this);
	}

	/**
	 * f0 -> <TEXT>
	 */
	public void visit(Id n) {
		n.f0.accept(this);
	}

	/**
	 * f0 -> "=="
	 *       | "!="
	 *       | "<"
	 *       | ">"
	 *       | "<="
	 *       | ">="
	 */
	public void visit(Condition n) {
		n.f0.accept(this);
	}

	/**
	 * f0 -> <TEXT>
	 */
	public void visit(Value n) {
		n.f0.accept(this);
	}

	/**
	 * f0 -> "and"
	 *       | "or"
	 */
	public void visit(Conjunction n) {
		n.f0.accept(this);
	}
}
