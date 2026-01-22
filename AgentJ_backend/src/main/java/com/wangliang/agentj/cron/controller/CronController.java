package com.wangliang.agentj.cron.controller;

import com.wangliang.agentj.cron.service.CronService;
import com.wangliang.agentj.cron.vo.CronConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cron-tasks")
@CrossOrigin(origins = "*") // Add cross-origin support
public class CronController {

	@Autowired
	private CronService cronService;

	@GetMapping
	public ResponseEntity<List<CronConfig>> getAllCronTasks() {
		return ResponseEntity.ok(cronService.getAllCronTasks());
	}

	@GetMapping("/{id}")
	public ResponseEntity<CronConfig> getCronTaskById(@PathVariable("id") String id) {
		return ResponseEntity.ok(cronService.getCronTaskById(id));
	}

	@PostMapping
	public ResponseEntity<CronConfig> createCronTask(@RequestBody CronConfig cronConfig) {
		return ResponseEntity.ok(cronService.createCronTask(cronConfig));
	}

	@PutMapping("/{id}")
	public ResponseEntity<CronConfig> updateCronTask(@PathVariable("id") Long id, @RequestBody CronConfig cronConfig) {
		cronConfig.setId(id);
		return ResponseEntity.ok(cronService.updateCronTask(cronConfig));
	}

	@PutMapping("/{id}/status")
	public ResponseEntity<Void> updateTaskStatus(@PathVariable("id") String id,
			@RequestParam("status") Integer status) {
		cronService.updateTaskStatus(id, status);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/{id}/execute")
	public ResponseEntity<Void> executeCronTask(@PathVariable("id") String id) {
		try {
			cronService.executeCronTask(id);
			return ResponseEntity.ok().build();
		}
		catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().build();
		}
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteCronTask(@PathVariable("id") String id) {
		try {
			cronService.deleteCronTask(id);
			return ResponseEntity.ok().build();
		}
		catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().build();
		}
	}

}
