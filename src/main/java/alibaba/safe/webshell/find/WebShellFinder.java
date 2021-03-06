package alibaba.safe.webshell.find;

import java.io.IOException;
import java.util.Iterator;

import com.aliyun.odps.OdpsException;
import com.aliyun.odps.data.Record;
import com.aliyun.odps.data.TableInfo;
import com.aliyun.odps.mapred.JobClient;
import com.aliyun.odps.mapred.MapperBase;
import com.aliyun.odps.mapred.ReducerBase;
import com.aliyun.odps.mapred.conf.JobConf;
import com.aliyun.odps.mapred.utils.InputUtils;
import com.aliyun.odps.mapred.utils.OutputUtils;
import com.aliyun.odps.mapred.utils.SchemaUtils;

public class WebShellFinder {

	public static class TestMapper extends MapperBase {
		private Record key;
		private Record value;

		@Override
		public void setup(TaskContext context) throws IOException {
			key = context.createMapOutputKeyRecord();
			value = context.createMapOutputValueRecord();
		}

		@Override
		public void map(long recordNum, Record record, TaskContext context)
				throws IOException {
			// id,url,post_data
			key.set("id", record.getString(0));
			//value.set("url", record.getString(1));
			value.set("post_data", record.getString(2));
			context.write(key, value);
		}

	}

	public static class TestReducer extends ReducerBase {

		Record output;

		@Override
		public void setup(TaskContext context) throws IOException {
			output = context.createOutputRecord();
		}

		// key:id value:post_data
		@Override
		public void reduce(Record key, Iterator<Record> values,
				TaskContext context) throws IOException {

			PostDataFilter postDataFilter = PostDataFilter.getInstance();
			String id = key.getString("id");
			
			while (values.hasNext()) {
				Record val = values.next();
				String postData = val.getString("post_data");
				if(postDataFilter.isWebShell(postData)){
					output.set(0, id);
					context.write(output);
				}
			}
		}
		
	}
	
	public static void main(String[] args) throws OdpsException {

		if (args.length != 2) {
			System.err
					.println("Usage: PlaysCount <user_actions_combine> <user_actions_count_filter>");
			System.exit(2);
		}

		JobConf job = new JobConf();
		job.setMapperClass(TestMapper.class);
		job.setReducerClass(TestReducer.class);

		job.setMapOutputKeySchema(SchemaUtils.fromString("id:string"));
		job.setMapOutputValueSchema(SchemaUtils.fromString("post_data:string"));

		InputUtils.addTable(TableInfo.builder().tableName(args[0]).build(), job);
		OutputUtils.addTable(TableInfo.builder().tableName(args[1]).build(),job);

		JobClient.runJob(job);
	}

}
