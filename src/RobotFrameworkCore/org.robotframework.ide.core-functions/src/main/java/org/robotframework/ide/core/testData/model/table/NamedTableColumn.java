package org.robotframework.ide.core.testData.model.table;

import org.robotframework.ide.core.testData.model.AModelElement;
import org.robotframework.ide.core.testData.model.LineElement;
import org.robotframework.ide.core.testData.model.LineElement.ElementType;
import org.robotframework.ide.core.testData.model.RobotLine;


public class NamedTableColumn extends AModelElement {

    public NamedTableColumn(RobotLine containingLine,
            LineElement originalElement) {
        super(ElementType.TABLE_COLUMN_NAME, containingLine, originalElement);
    }
}
