/*
 * {{{ header & license
 * Copyright (c) 2007 Wisconsin Court System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package org.xhtmlrenderer.newtable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.render.RenderingContext;

public class TableSectionBox extends BlockBox {
    private List _grid = new ArrayList();
    
    public TableSectionBox() {
    }
    
    public List getGrid() {
        return _grid;
    }

    public void setGrid(List grid) {
        _grid = grid;
    }
    
    public void extendGridToColumnCount(int columnCount) {
        for (Iterator i = _grid.iterator(); i.hasNext(); ) {
            RowData row = (RowData)i.next();
            row.extendToColumnCount(columnCount);
        }
    }
    
    public void splitColumn(int pos) {
        for (Iterator i = _grid.iterator(); i.hasNext(); ) {
            RowData row = (RowData)i.next();
            row.splitColumn(pos);
        }
    }
    
    public void recalcCells(LayoutContext c) {
        int cRow = 0;
        _grid.clear();
        ensureChildren(c);
        for (Iterator i = getChildIterator(); i.hasNext(); cRow++) {
            TableRowBox row = (TableRowBox)i.next();
            row.ensureChildren(c);
            for (Iterator j = row.getChildIterator(); j.hasNext(); ) {
                TableCellBox cell = (TableCellBox)j.next();
                addCell(row, cell, cRow);
            }
            setTableRow(cRow, row);
        }
    }
    
    public void calcBorders(LayoutContext c) {
        ensureChildren(c);
        for (Iterator i = getChildIterator(); i.hasNext(); ) {
            TableRowBox row = (TableRowBox)i.next();
            row.ensureChildren(c);
            for (Iterator j = row.getChildIterator(); j.hasNext(); ) {
                TableCellBox cell = (TableCellBox)j.next();
                cell.calcCollapsedBorder(c);
            }
        }
    }
    
    private void setTableRow(int row, TableRowBox tableRow) {
        ((RowData)_grid.get(row)).setTableRow(tableRow);
    }
    
    public TableCellBox cellAt(int row, int col) {
        return (TableCellBox)((RowData)_grid.get(row)).getRow().get(col);
    }
    
    private void setCellAt(int row, int col, TableCellBox cell) {
        ((RowData)_grid.get(row)).getRow().set(col, cell);
    }
    
    private void ensureRows(int numRows) {
        int nRows = _grid.size();
        int nCols = getTable().numEffCols();
        
        while (nRows < numRows) {
            RowData row = new RowData();
            row.extendToColumnCount(nCols);
            _grid.add(row);
            nRows++;
        }
    }
    
    private TableBox getTable() {
        return (TableBox)getParent();
    }
    
    private void addCell(TableRowBox row, TableCellBox cell, int cRow) {
        int rSpan = cell.getStyle().getRowSpan();
        int cSpan = cell.getStyle().getColSpan();
        
        List columns = getTable().getColumns();
        int nCols = columns.size();
        int cCol = 0;
        
        ensureRows(cRow + rSpan);
        
        while ( cCol < nCols && cellAt(cRow, cCol) != null) {
            cCol++;
        }
        
        int col = cCol;
        TableCellBox set = cell;
        while (cSpan > 0) {
            int currentSpan;
            if (cCol >= nCols) {
                getTable().appendColumn(cSpan);
                currentSpan = cSpan;
            } else {
                ColumnData cData = (ColumnData)columns.get(cCol);
                if (cSpan < cData.getSpan()) {
                    getTable().splitColumn(cCol, cSpan);
                }
                cData = (ColumnData)columns.get(cCol);
                currentSpan = cData.getSpan();
            }
            
            int r = 0;
            while (r < rSpan) {
                if (cellAt(cRow + r, cCol) == null) {
                    setCellAt(cRow + r, cCol, set);
                }
                r++;
            }
            cCol++;
            cSpan -= currentSpan;
            set = TableCellBox.SPANNING_CELL;
        }
        
        cell.setRow(cRow);
        cell.setCol(getTable().effColToCol(col));
    }
    
    public void reset(LayoutContext c) {
        super.reset(c);
        _grid.clear();
    }
    
    void setCellWidths(LayoutContext c)
    {
        int[] columnPos = getTable().getColumnPos();
        
        for (Iterator i = _grid.iterator(); i.hasNext(); ) {
            RowData row = (RowData)i.next();
            List cols = row.getRow();
            int hspacing = getTable().getStyle().getBorderHSpacing(c);
            for (int j = 0; j < cols.size(); j++) {
                TableCellBox cell = (TableCellBox)cols.get(j);
                
                if (cell == null || cell == TableCellBox.SPANNING_CELL) {
                    continue;
                }
                
                int endCol = j;
                int cspan = cell.getStyle().getColSpan();
                while (cspan > 0 && endCol < cols.size()) {
                    cspan -= getTable().spanOfEffCol(endCol);
                    endCol++;
                }
                
                int w = columnPos[endCol] - columnPos[j] - hspacing;
                cell.setLayoutWidth(c, w);
                cell.setX(columnPos[j] + hspacing);
            }
        }
    }
    
    public boolean isAutoHeight() {
        // FIXME Should properly handle absolute heights (%s resolve to auto)
        return true;
    }
    
    public int numRows() { 
        return _grid.size(); 
    }
    
    protected boolean isSkipWhenCollapsingMargins() {
        return true;
    }
    
    public void paintBorder(RenderingContext c) {
        // row groups never have borders
    }
    
    public void paintBackground(RenderingContext c) {
        // painted at the cell level
    }
    
    public TableRowBox getLastRow() {
        if (getChildCount() > 0) {
            return (TableRowBox)getChild(getChildCount()-1);
        } else {
            return null;
        }
    }
}
