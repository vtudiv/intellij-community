// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.structuralsearch.plugin.ui;

import com.intellij.lang.Language;
import com.intellij.lang.LanguageUtil;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.structuralsearch.StructuralSearchProfile;
import com.intellij.structuralsearch.StructuralSearchProfileBase;
import com.intellij.structuralsearch.StructuralSearchUtil;
import com.intellij.ui.ComboboxSpeedSearch;
import com.intellij.ui.LayeredIcon;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;

/**
 * @author Pavel.Dolgov
 */
public class FileTypeSelector extends ComboBox<FileTypeInfo> {

  public FileTypeSelector() {
    super(createModel());
    setRenderer(new MyCellRenderer());
    new MySpeedSearch(this);
  }

  @Nullable
  @Override
  public FileTypeInfo getSelectedItem() {
    return (FileTypeInfo)super.getSelectedItem();
  }

  @Nullable
  public FileType getSelectedFileType() {
    FileTypeInfo info = (FileTypeInfo)super.getSelectedItem();
    return info != null ? info.getFileType() : null;
  }

  public void setSelectedItem(@NotNull FileType type, @Nullable Language dialect, @Nullable String context) {
    DefaultComboBoxModel<FileTypeInfo> model = (DefaultComboBoxModel<FileTypeInfo>)getModel();
    for (int i = 0; i < model.getSize(); i++) {
      FileTypeInfo info = model.getElementAt(i);
      if (info.isEqualTo(type, dialect, context)) {
        setSelectedItem(info);
        return;
      }
    }
  }

  @Override
  public void setSelectedItem(Object anObject) {
    if (anObject instanceof FileTypeInfo) {
      final FileTypeInfo selectedInfo = (FileTypeInfo)anObject;
      if (!selectedInfo.isEnabled()) {
        final MyComboBoxModel model = (MyComboBoxModel)getModel();
        final int index = model.getIndexOf(selectedInfo);
        if (index >= 0 && index + 1 < model.getSize()) {
          final FileTypeInfo nextInfo = model.getElementAt(index + 1);
          super.setSelectedItem(nextInfo);
          return;
        }
      }
    }
    super.setSelectedItem(anObject);
  }

  @NotNull
  private static DefaultComboBoxModel<FileTypeInfo> createModel() {
    final List<FileType> types = new ArrayList<>();
    for (FileType fileType : StructuralSearchUtil.getSuitableFileTypes()) {
      if (StructuralSearchUtil.getProfileByFileType(fileType) != null) {
        types.add(fileType);
      }
    }
    Collections.sort(types, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
    final List<FileTypeInfo> infos = new ArrayList<>();
    for (FileType fileType : types) {
      final boolean duplicated = isDuplicated(fileType, types);

      final StructuralSearchProfile profile = StructuralSearchUtil.getProfileByFileType(fileType);
      assert profile != null;
      if (profile instanceof StructuralSearchProfileBase) {
        final String[] contextNames = ((StructuralSearchProfileBase)profile).getContextNames();
        if (contextNames.length != 0) {
          infos.add(new FileTypeInfo(fileType, null, null, false, duplicated));
          for (String contextName: contextNames) {
            infos.add(new FileTypeInfo(fileType, null, contextName, true, duplicated));
          }
          continue; // proceed with the next file type
        }
      }

      infos.add(new FileTypeInfo(fileType, null, null, true, duplicated));

      if (fileType instanceof LanguageFileType) {
        final Language language = ((LanguageFileType)fileType).getLanguage();
        final Language[] languageDialects = LanguageUtil.getLanguageDialects(language);
        Arrays.sort(languageDialects, Comparator.comparing(Language::getDisplayName));
        for (Language dialect : languageDialects) {
          if (profile.isMyLanguage(dialect)) {
            infos.add(new FileTypeInfo(fileType, dialect, null, true, duplicated));
          }
        }
      }
    }

    return new MyComboBoxModel(infos);
  }

  private static boolean isDuplicated(@NotNull FileType fileType, @NotNull List<FileType> types) {
    String description = fileType.getDescription();
    for (FileType type : types) {
      if (type != fileType && description.equals(type.getDescription())) {
        return true;
      }
    }
    return false;
  }

  private static class MyComboBoxModel extends DefaultComboBoxModel<FileTypeInfo> {
    MyComboBoxModel(List<FileTypeInfo> infos) {
      super(infos.toArray(FileTypeInfo.EMPTY_ARRAY));
    }
  }

  private static class MyCellRenderer extends SimpleListCellRenderer<FileTypeInfo> {
    private static final Icon EMPTY_ICON = EmptyIcon.ICON_18;
    private static final Icon WIDE_EMPTY_ICON = JBUI.scale(EmptyIcon.create(32, 18));

    @Override
    public void customize(JList<? extends FileTypeInfo> list, FileTypeInfo value, int index, boolean selected, boolean hasFocus) {
      if (value == null) {
        return;
      }
      if (value.isNested() && index >= 0) {
        setIcon(WIDE_EMPTY_ICON);
        setText(value.getText());
      }
      else {
        setIcon(getFileTypeIcon(value));
        setText(value.getFullText());
      }
    }

    @NotNull
    private static Icon getFileTypeIcon(FileTypeInfo info) {
      LayeredIcon layeredIcon = new LayeredIcon(2);
      layeredIcon.setIcon(EMPTY_ICON, 0);
      Icon icon = info.getFileType().getIcon();
      if (icon != null) {
        layeredIcon.setIcon(icon, 1);
      }
      return layeredIcon;
    }
  }

  private static class MySpeedSearch extends ComboboxSpeedSearch {
    MySpeedSearch(FileTypeSelector comboBox) {super(comboBox);}

    @Override
    protected String getElementText(Object element) {
      return ((FileTypeInfo)element).getSearchText();
    }
  }
}
