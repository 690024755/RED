/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.model.table.exec.descs.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.rf.ide.core.testdata.model.RobotFileOutput.BuildMessage;
import org.rf.ide.core.testdata.model.table.RobotExecutableRow;
import org.rf.ide.core.testdata.model.table.exec.descs.IExecutableRowDescriptor;
import org.rf.ide.core.testdata.model.table.exec.descs.ast.mapping.IElementDeclaration;
import org.rf.ide.core.testdata.model.table.exec.descs.ast.mapping.VariableDeclaration;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;

public class ForLoopDeclarationRowDescriptor<T> implements IExecutableRowDescriptor<T> {

    private static final List<RobotToken> KEYWORD_ARGUMENTS = Collections
            .unmodifiableList(new ArrayList<RobotToken>(0));

    private final List<VariableDeclaration> createdVariables = new ArrayList<>();

    private RobotToken forAction = new RobotToken();

    private RobotToken inAction = new RobotToken();

    private final List<VariableDeclaration> usedVariables = new ArrayList<>();

    private final List<IElementDeclaration> textParameters = new ArrayList<>();

    private final List<BuildMessage> messages = new ArrayList<>();

    private final RobotExecutableRow<T> row;

    public ForLoopDeclarationRowDescriptor(final RobotExecutableRow<T> row) {
        this.row = row;
    }

    @Override
    public List<VariableDeclaration> getCreatedVariables() {
        return Collections.unmodifiableList(createdVariables);
    }

    public void addCreatedVariables(final List<VariableDeclaration> variables) {
        for (final VariableDeclaration var : variables) {
            addCreatedVariable(var);
        }
    }

    public void addCreatedVariable(final VariableDeclaration variable) {
        this.createdVariables.add(variable);
    }

    @Override
    public RobotToken getAction() {
        return forAction;
    }

    public void setAction(final RobotToken forAction) {
        this.forAction = forAction;
    }

    @Override
    public RobotToken getKeywordAction() {
        return getAction();
    }

    public RobotToken getInAction() {
        return inAction;
    }

    public void setInAction(final RobotToken inAction) {
        this.inAction = inAction;
    }

    @Override
    public List<VariableDeclaration> getUsedVariables() {
        return Collections.unmodifiableList(usedVariables);
    }

    public void addUsedVariables(final List<VariableDeclaration> variables) {
        for (final VariableDeclaration var : variables) {
            addUsedVariable(var);
        }
    }

    public void addUsedVariable(final VariableDeclaration variable) {
        this.usedVariables.add(variable);
    }

    @Override
    public List<IElementDeclaration> getTextParameters() {
        return Collections.unmodifiableList(textParameters);
    }

    public void addTextParameters(final List<IElementDeclaration> textParams) {
        for (final IElementDeclaration tP : textParams) {
            addTextParameter(tP);
        }
    }

    public void addTextParameter(final IElementDeclaration textParam) {
        this.textParameters.add(textParam);
    }

    @Override
    public List<BuildMessage> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public void addMessages(final List<BuildMessage> msgs) {
        for (final BuildMessage bm : msgs) {
            addMessage(bm);
        }
    }

    public void addMessage(final BuildMessage bm) {
        messages.add(bm);
    }

    @Override
    public RowType getRowType() {
        return RowType.FOR;
    }

    @Override
    public RobotExecutableRow<T> getRow() {
        return row;
    }

    @Override
    public List<RobotToken> getKeywordArguments() {
        return KEYWORD_ARGUMENTS;
    }
}
