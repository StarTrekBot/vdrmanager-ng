package de.bjusystems.vdrmanager.ng.utils.svdrp;

import java.util.List;

public interface SvdrpFinishedListener<Result> {

	public void finished(List<Result> results);

}
