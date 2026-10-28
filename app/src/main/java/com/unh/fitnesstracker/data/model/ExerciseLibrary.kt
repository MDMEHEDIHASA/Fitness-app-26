package com.unh.fitnesstracker.data.model

import com.unh.fitnesstracker.data.model.MuscleGroup.ARMS
import com.unh.fitnesstracker.data.model.MuscleGroup.BACK
import com.unh.fitnesstracker.data.model.MuscleGroup.CARDIO
import com.unh.fitnesstracker.data.model.MuscleGroup.CHEST
import com.unh.fitnesstracker.data.model.MuscleGroup.CORE
import com.unh.fitnesstracker.data.model.MuscleGroup.LEGS
import com.unh.fitnesstracker.data.model.MuscleGroup.SHOULDERS

/**
 * Built-in exercises shipped with the app. IDs are stable strings because saved workouts and
 * templates refer to them; never rename an ID once released.
 */
object ExerciseLibrary {

    val all: List<Exercise> = listOf(
        reps("bench_press", "Bench Press", CHEST),
        reps("incline_bench_press", "Incline Bench Press", CHEST),
        reps("dumbbell_bench_press", "Dumbbell Bench Press", CHEST),
        reps("chest_fly", "Chest Fly", CHEST),
        reps("push_up", "Push-up", CHEST),
        reps("chest_dip", "Chest Dip", CHEST),

        reps("deadlift", "Deadlift", BACK),
        reps("pull_up", "Pull-up", BACK),
        reps("chin_up", "Chin-up", BACK),
        reps("lat_pulldown", "Lat Pulldown", BACK),
        reps("barbell_row", "Barbell Row", BACK),
        reps("dumbbell_row", "Dumbbell Row", BACK),
        reps("seated_cable_row", "Seated Cable Row", BACK),

        reps("back_squat", "Back Squat", LEGS),
        reps("front_squat", "Front Squat", LEGS),
        reps("leg_press", "Leg Press", LEGS),
        reps("romanian_deadlift", "Romanian Deadlift", LEGS),
        reps("lunge", "Lunge", LEGS),
        reps("bulgarian_split_squat", "Bulgarian Split Squat", LEGS),
        reps("leg_extension", "Leg Extension", LEGS),
        reps("leg_curl", "Leg Curl", LEGS),
        reps("calf_raise", "Calf Raise", LEGS),
        reps("hip_thrust", "Hip Thrust", LEGS),

        reps("overhead_press", "Overhead Press", SHOULDERS),
        reps("dumbbell_shoulder_press", "Dumbbell Shoulder Press", SHOULDERS),
        reps("lateral_raise", "Lateral Raise", SHOULDERS),
        reps("face_pull", "Face Pull", SHOULDERS),
        reps("rear_delt_fly", "Rear Delt Fly", SHOULDERS),

        reps("barbell_curl", "Barbell Curl", ARMS),
        reps("dumbbell_curl", "Dumbbell Curl", ARMS),
        reps("hammer_curl", "Hammer Curl", ARMS),
        reps("triceps_pushdown", "Triceps Pushdown", ARMS),
        reps("skull_crusher", "Skull Crusher", ARMS),
        reps("triceps_dip", "Triceps Dip", ARMS),

        time("plank", "Plank", CORE),
        reps("crunch", "Crunch", CORE),
        reps("hanging_leg_raise", "Hanging Leg Raise", CORE),
        reps("russian_twist", "Russian Twist", CORE),
        reps("ab_wheel", "Ab Wheel Rollout", CORE),

        time("rowing_machine", "Rowing Machine", CARDIO),
        time("jump_rope", "Jump Rope", CARDIO),
        reps("burpee", "Burpee", CARDIO),
        reps("kettlebell_swing", "Kettlebell Swing", CARDIO),
    )

    private fun reps(id: String, name: String, group: MuscleGroup) = Exercise(id, name, group, TrackedBy.REPS)
    private fun time(id: String, name: String, group: MuscleGroup) = Exercise(id, name, group, TrackedBy.TIME)
}
