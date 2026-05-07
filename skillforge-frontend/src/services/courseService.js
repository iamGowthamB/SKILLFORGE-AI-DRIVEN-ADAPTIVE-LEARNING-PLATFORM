import api from './api'

export const courseService = {
  // async getAllCourses(studentId = null) {
  //   const params = studentId ? { studentId } : {}
  //   const response = await api.get('/courses', { params })
  //   return response.data
  // },
  async getAllCourses(params = {}) {
    const response = await api.get("/courses", { params });
    return response;
  },

  async getPublishedCourses(studentId = null) {
    const params = studentId ? { studentId } : {}
    const response = await api.get('/courses/published', { params })
    console.log("courseService.getPublishedCourses response:", response)
    return response.data
  },

  // async getCourseById(id, studentId = null) {
  //   const params = studentId ? { studentId } : {}
  //   const response = await api.get(`/courses/${id}`, { params })
  //   return response.data
  // },
  async getCourseById(courseId, studentId) {
    if (studentId) {
      return api.get(`/courses/${courseId}?studentId=${studentId}`);
    }
    return api.get(`/courses/${courseId}`);
  },

  // async getInstructorCourses(instructorId) {
  //   const response = await api.get(`/courses/instructor/${instructorId}`)
  //   return response
  // },
  async getInstructorCourses(instructorId) {
    const response = await api.get(`/courses/instructor/${instructorId}`);

    // 🟢 FIX: Instructor API returns wrapper { success, message, data: [...] }
    return response.data?.data ?? [];
  },

  async createCourse(courseData, instructorId) {
    const response = await api.post(`/courses?instructorId=${instructorId}`, courseData)
    return response.data
  },

  async updateCourse(id, courseData) {
    const response = await api.put(`/courses/${id}`, courseData)
    return response.data
  },

  async publishCourse(id) {
    const response = await api.patch(`/courses/${id}/publish`)
    return response.data
  },

  async deleteCourse(id) {
    const response = await api.delete(`/courses/${id}`)
    return response.data
  },
  async getFilteredCourses(params) {
    const response = await api.get('/courses', { params });
    return response.data;
  },

  async getRecommendedCourses(studentId) {
    const response = await api.get(`/courses/recommendations?studentId=${studentId}`);
    return response.data;
  },

  async getNextCourseRecommendation(courseId, studentId) {
    const params = studentId ? { studentId } : {};
    const response = await api.get(`/courses/${courseId}/recommendation`, { params });
    return response.data;
  }

}

